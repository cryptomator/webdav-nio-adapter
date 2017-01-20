package org.cryptomator.frontend.webdav.servlet;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.Normalizer;
import java.text.Normalizer.Form;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

/**
 * Makes sure, all resource paths containing special unicode characters are composed of characters in {@link Form#NFC Normalization Form C}.
 */
public class UnicodeResourcePathNormalizationFilter implements HttpFilter {

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// no-op
	}

	@Override
	public void doFilterHttp(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
		chain.doFilter(new NormalizedRequest(request), response);
	}

	@Override
	public void destroy() {
		// no-op
	}

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

}
