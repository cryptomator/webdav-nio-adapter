/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.frontend.webdav.servlet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.text.Normalizer.Form;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.xml.namespace.QName;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.util.EncodeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Makes sure, all resource paths containing special unicode characters are composed of characters in {@link Form#NFC Normalization Form C}.
 * Multistatus responses will be transformed back to {@link Form#NFD NFD} for certain user agents known to expect it.
 */
public class UnicodeResourcePathNormalizationFilter implements HttpFilter {

	private static final Logger LOG = LoggerFactory.getLogger(UnicodeResourcePathNormalizationFilter.class);
	private static final String PROPFIND_METHOD = "PROPFIND";
	private static final String USER_AGENT_HEADER = "User-Agent";
	private static final String[] USER_AGENTS_EXPECTING_NFD = {"WebDAVFS"};

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// no-op
	}

	@Override
	public void doFilterHttp(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
		HttpServletResponse filteredResponse;
		if (PROPFIND_METHOD.equalsIgnoreCase(request.getMethod()) && isUserAgentExpectingNfdResponses(request)) {
			// response will probably be a multi status xml response, we need to filter it, too:
			filteredResponse = new NormalizedMultiStatusResponse(response, Form.NFD);
		} else {
			// otherwise don't intercept the response
			filteredResponse = response;
		}
		chain.doFilter(new NormalizedRequest(request), filteredResponse);
	}

	private boolean isUserAgentExpectingNfdResponses(HttpServletRequest request) {
		String userAgent = request.getHeader(USER_AGENT_HEADER);
		return StringUtils.containsAny(userAgent, USER_AGENTS_EXPECTING_NFD);
	}

	@Override
	public void destroy() {
		// no-op
	}

	/**
	 * Encodes any paths in this requests to {@link Form#NFC}.
	 */
	private static class NormalizedRequest extends HttpServletRequestWrapper {

		private static final String DESTINATION_HEADER = "Destination";

		public NormalizedRequest(HttpServletRequest request) {
			super(request);
		}

		@Override
		public String getPathInfo() {
			return Normalizer.normalize(super.getPathInfo(), Form.NFC);
		}

		@Override
		public String getRequestURI() {
			try {
				return new URI(null, null, getContextPath() + getPathInfo(), null).toString();
			} catch (URISyntaxException e) {
				throw new IllegalStateException(e);
			}
		}

		@Override
		public String getHeader(String name) {
			if (DESTINATION_HEADER.equalsIgnoreCase(name)) {
				String origDestHeader = super.getHeader(DESTINATION_HEADER);
				if (origDestHeader == null) {
					return null;
				}
				try {
					// header value contains RFC 2396 absolute uri
					URI orig = URI.create(origDestHeader);
					String normalizedPath = Normalizer.normalize(orig.getPath(), Form.NFC);
					return new URI(orig.getScheme(), orig.getUserInfo(), orig.getHost(), orig.getPort(), normalizedPath, orig.getQuery(), orig.getFragment()).toString();
				} catch (URISyntaxException e) {
					throw new IllegalStateException("URI constructed from valid URI can not be invalid.", e);
				}
			} else {
				return super.getHeader(name);
			}
		}

		@Override
		public StringBuffer getRequestURL() {
			StringBuffer url = new StringBuffer();
			url.append(getScheme()).append("://");
			url.append(getServerName());
			if ((getScheme().equals("http") && getServerPort() != 80) || (getScheme().equals("https") && getServerPort() != 443)) {
				url.append(':').append(getServerPort());
			}
			url.append(getRequestURI());
			return url;
		}

	}

	/**
	 * Whenever the http status code is 207 and a fixed-length body is expected, this ServletResponse will return a filtered outputstream.
	 */
	private static class NormalizedMultiStatusResponse extends HttpServletResponseWrapper {

		private boolean isMultiStatus = true;
		private int contentLength = -1;
		private final Form normalizationForm;

		public NormalizedMultiStatusResponse(HttpServletResponse response, Form normalizationForm) {
			super(response);
			this.normalizationForm = normalizationForm;
		}

		@Override
		public void setStatus(int sc) {
			super.setStatus(sc);
			isMultiStatus = sc == DavServletResponse.SC_MULTI_STATUS;
		}

		@Override
		public void setContentLength(int len) {
			contentLength = len;
		}

		@Override
		public void setContentLengthLong(long len) {
			if (len <= Integer.MAX_VALUE) {
				contentLength = (int) len;
			} else {
				// we do not want to intercept a 4gb+ response. just stream the original, unfiltered response.
				contentLength = -1;
			}
		}

		@Override
		public ServletOutputStream getOutputStream() throws IOException {
			if (isMultiStatus && contentLength != -1) {
				return new NormalizedServletOutputStream(super.getOutputStream(), contentLength, normalizationForm);
			} else {
				LOG.warn("Response not a Multi Status response, thus output encoding will not be normalized.");
				return super.getOutputStream();
			}
		}

	}

	/**
	 * Buffers all bytes up to a pre-defined threshold (the content length).
	 * When it is reached, the buffer will be transformed using a {@link MultistatusHrefNormalizer}.
	 */
	private static class NormalizedServletOutputStream extends ServletOutputStream {

		private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		private final ServletOutputStream delegate;
		private final int contentLength;
		private final Form normalizationForm;

		public NormalizedServletOutputStream(ServletOutputStream delegate, int contentLength, Form normalizationForm) {
			if (contentLength < 0) {
				throw new IllegalArgumentException("contentLength must be a positive integer");
			}
			this.delegate = delegate;
			this.contentLength = contentLength;
			this.normalizationForm = normalizationForm;
		}

		@Override
		public boolean isReady() {
			return delegate.isReady();
		}

		@Override
		public void setWriteListener(WriteListener writeListener) {
			delegate.setWriteListener(writeListener);
		}

		@Override
		public void write(int b) throws IOException {
			buffer.write(b);
		}

		@Override
		public void write(byte b[], int off, int len) throws IOException {
			buffer.write(b, off, len);
			if (buffer.size() >= contentLength) {
				normalize();
			}
		}

		private void normalize() throws IOException {
			try (InputStream in = new ByteArrayInputStream(buffer.toByteArray(), 0, contentLength); //
					MultistatusHrefNormalizer transformer = new MultistatusHrefNormalizer(in, delegate, normalizationForm)) {
				transformer.transform();
			} catch (XMLStreamException e) {
				LOG.error("Error processing XML.", e);
				throw new IOException(e);
			}
		}

	}

	/**
	 * Parses XML from a given input stream and replicates it to the given output stream, except for any href tags whose contents will be interpreted as URIs
	 * and adjusted to the specified Unicode Normalization Form.
	 */
	// visible for testing
	static class MultistatusHrefNormalizer implements AutoCloseable {

		private final XMLStreamReader reader;
		private final XMLStreamWriter writer;
		private boolean isParsingHref = false;
		private final Form normalizationForm;

		public MultistatusHrefNormalizer(InputStream in, OutputStream out, Form normalizationForm) {
			try {
				XMLInputFactory inputFactory = XMLInputFactory.newInstance();
				inputFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
				this.reader = inputFactory.createXMLStreamReader(in);
				XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
				this.writer = outputFactory.createXMLStreamWriter(out, StandardCharsets.UTF_8.name());
			} catch (XMLStreamException | FactoryConfigurationError e) {
				throw new IllegalStateException("Failed to set up XML reader/writer", e);
			}
			this.normalizationForm = normalizationForm;
		}

		public void transform() throws XMLStreamException {
			writer.writeStartDocument();
			while (reader.hasNext()) {
				int xmlEvent = reader.next();
				switch (xmlEvent) {
				case XMLStreamReader.START_ELEMENT:
					QName qname = reader.getName();
					writer.writeStartElement(qname.getPrefix(), qname.getLocalPart(), qname.getNamespaceURI());
					for (int i = 0; i < reader.getNamespaceCount(); i++) {
						writer.writeNamespace(reader.getNamespacePrefix(i), reader.getNamespaceURI(i));
					}
					isParsingHref = qname.getLocalPart().equalsIgnoreCase("href");
					break;
				case XMLStreamReader.CHARACTERS:
					if (isParsingHref) {
						writer.writeCharacters(transformHref(reader.getText()));
					} else {
						writer.writeCharacters(reader.getText());
					}
					break;
				case XMLStreamReader.END_ELEMENT:
					writer.writeEndElement();
					isParsingHref = false;
					break;
				default:
					break;
				}
			}
			writer.writeEndDocument();
			writer.flush();
		}

		private String transformHref(String originalHref) {
			URI uri = URI.create(originalHref); // should be a valid RFC 2396 URI
			String normalizedPath = Normalizer.normalize(uri.getPath(), normalizationForm);
			String escapedPath = EncodeUtil.escapePath(normalizedPath);
			return uri.getScheme() + "://" + uri.getRawAuthority() + escapedPath;
		}

		@Override
		public void close() throws XMLStreamException {
			reader.close();
			writer.close();
		}

	}

}
