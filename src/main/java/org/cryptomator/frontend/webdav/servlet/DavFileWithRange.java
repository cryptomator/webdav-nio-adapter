/*******************************************************************************
 * Copyright (c) 2016, 2017 Sebastian Stenzel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE.txt.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.frontend.webdav.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.Optional;

import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.io.OutputContext;
import org.apache.jackrabbit.webdav.lock.LockManager;
import org.eclipse.jetty.http.HttpHeader;

import com.google.common.io.ByteStreams;

/**
 * Delivers only the requested range of bytes from a file.
 * 
 * @see <a href="https://tools.ietf.org/html/rfc7233#section-4"/>RFC 7233 Section 4</a>
 */
class DavFileWithRange extends DavFile {

	private final ByteRange reqRange;

	public DavFileWithRange(DavResourceFactoryImpl factory, LockManager lockManager, DavLocatorImpl locator, Path path, BasicFileAttributes attr, DavSession session, ByteRange byteRange) {
		super(factory, lockManager, locator, path, Optional.of(attr), session);
		this.reqRange = Objects.requireNonNull(byteRange);
	}

	@Override
	public void spool(OutputContext outputContext) throws IOException {
		assert exists();
		outputContext.setModificationTime(attr.get().lastModifiedTime().toMillis());
		if (!outputContext.hasStream()) {
			return;
		}
		final long contentLength = attr.get().size();
		final long firstByte = reqRange.getEffectiveFirstByte(contentLength);
		final long lastByte = reqRange.getEffectiveLastByte(contentLength);
		final long rangeLength = lastByte - firstByte + 1;
		assert firstByte >= 0;
		assert lastByte < contentLength;
		if (firstByte >= contentLength) {
			outputContext.setProperty(HttpHeader.CONTENT_RANGE.asString(), "bytes */" + contentLength);
			throw new UncheckedDavException(DavServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE, "Valid Range would be in [0, " + contentLength + "]");
		}
		outputContext.setContentLength(rangeLength);
		outputContext.setProperty(HttpHeader.CONTENT_RANGE.asString(), contentRangeResponseHeader(firstByte, lastByte, contentLength));
		outputContext.setContentType(CONTENT_TYPE_VALUE);
		outputContext.setProperty(CONTENT_DISPOSITION_HEADER, CONTENT_DISPOSITION_VALUE);
		outputContext.setProperty(X_CONTENT_TYPE_OPTIONS_HEADER, X_CONTENT_TYPE_OPTIONS_VALUE);
		try (SeekableByteChannel src = Files.newByteChannel(path, StandardOpenOption.READ); OutputStream out = outputContext.getOutputStream()) {
			src.position(firstByte);
			InputStream limitedIn = ByteStreams.limit(Channels.newInputStream(src), rangeLength);
			ByteStreams.copy(limitedIn, out);
		}
	}

	private String contentRangeResponseHeader(long firstByte, long lastByte, long completeLength) {
		return String.format("bytes %d-%d/%d", firstByte, lastByte, completeLength);
	}

}
