/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
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

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
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

	private final Pair<String, String> requestRange;

	public DavFileWithRange(DavResourceFactoryImpl factory, LockManager lockManager, DavLocatorImpl locator, Path path, BasicFileAttributes attr, DavSession session, Pair<String, String> requestRange) {
		super(factory, lockManager, locator, path, Optional.of(attr), session);
		this.requestRange = Objects.requireNonNull(requestRange);
	}

	@Override
	public void spool(OutputContext outputContext) throws IOException {
		assert exists();
		outputContext.setModificationTime(attr.get().lastModifiedTime().toMillis());
		if (!outputContext.hasStream()) {
			return;
		}
		final long contentLength = attr.get().size();
		final Pair<Long, Long> range = getEffectiveRange(contentLength);
		if (range.getLeft() < 0 || range.getLeft() > range.getRight() || range.getRight() > contentLength) {
			outputContext.setProperty(HttpHeader.CONTENT_RANGE.asString(), "bytes */" + contentLength);
			throw new UncheckedDavException(DavServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE, "Valid Range would be in [0, " + contentLength + "]");
		}
		final Long rangeLength = range.getRight() - range.getLeft() + 1;
		outputContext.setContentLength(rangeLength);
		outputContext.setProperty(HttpHeader.CONTENT_RANGE.asString(), contentRangeResponseHeader(range.getLeft(), range.getRight(), contentLength));
		outputContext.setContentType(CONTENT_TYPE_VALUE);
		outputContext.setProperty(CONTENT_DISPOSITION_HEADER, CONTENT_DISPOSITION_VALUE);
		outputContext.setProperty(X_CONTENT_TYPE_OPTIONS_HEADER, X_CONTENT_TYPE_OPTIONS_VALUE);
		try (SeekableByteChannel src = Files.newByteChannel(path, StandardOpenOption.READ); OutputStream out = outputContext.getOutputStream()) {
			src.position(range.getLeft());
			InputStream limitedIn = ByteStreams.limit(Channels.newInputStream(src), rangeLength);
			ByteStreams.copy(limitedIn, out);
		}
	}

	private String contentRangeResponseHeader(long firstByte, long lastByte, long completeLength) {
		return String.format("bytes %d-%d/%d", firstByte, lastByte, completeLength);
	}

	private Pair<Long, Long> getEffectiveRange(long contentLength) {
		try {
			final Long lower = requestRange.getLeft().isEmpty() ? null : Long.valueOf(requestRange.getLeft());
			final Long upper = requestRange.getRight().isEmpty() ? null : Long.valueOf(requestRange.getRight());
			if (lower == null && upper == null) {
				return new ImmutablePair<Long, Long>(0l, contentLength - 1);
			} else if (lower == null) {
				return new ImmutablePair<Long, Long>(contentLength - upper, contentLength - 1);
			} else if (upper == null) {
				return new ImmutablePair<Long, Long>(lower, contentLength - 1);
			} else {
				return new ImmutablePair<Long, Long>(lower, Math.min(upper, contentLength - 1));
			}
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid byte range: " + requestRange, e);
		}
	}

}
