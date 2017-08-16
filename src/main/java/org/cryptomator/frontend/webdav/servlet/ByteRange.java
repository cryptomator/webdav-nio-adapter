/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.frontend.webdav.servlet;

import java.util.List;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;

/**
 * Parsed HTTP range header field (<a href="https://tools.ietf.org/html/rfc7233">RFC 7233</a>)<br/>
 * 
 * Valid ranges:<br/>
 * <code>
 * bytes=100-200<br/>
 * bytes=-500<br/>
 * bytes=1000-
 * </code>
 */
class ByteRange {

	private static final String RANGE_BYTE_PREFIX = "bytes=";
	private static final char RANGE_SET_SEP = ',';
	private static final char RANGE_SEP = '-';

	private final Long firstByte;
	private final Long lastByte;

	private ByteRange(Long firstByte, Long lastByte) throws MalformedByteRangeException {
		if (firstByte == null && lastByte == null || //
				firstByte != null && lastByte != null && firstByte > lastByte) {
			throw new MalformedByteRangeException();
		}
		this.firstByte = firstByte;
		this.lastByte = lastByte;
	}

	/**
	 * @param headerValue The raw HTTP header value (i.e. without the key, e.g. <code>bytes=100-200</code>)
	 * @throws UnsupportedRangeException thrown if the range is not supported by this implementation (range header should be ignored)
	 * @throws MalformedByteRangeException thrown if the range is syntactically malformed (client should be informed about a bad request)
	 */
	public static ByteRange parse(String headerValue) throws UnsupportedRangeException, MalformedByteRangeException {
		if (!headerValue.startsWith(RANGE_BYTE_PREFIX)) {
			throw new UnsupportedRangeException();
		}
		final String byteRangeStr = getSingleByteRange(headerValue);
		return getPositions(byteRangeStr);
	}

	private static String getSingleByteRange(String headerValue) throws UnsupportedRangeException, MalformedByteRangeException {
		final String byteRangeSet = headerValue.substring(RANGE_BYTE_PREFIX.length());
		if (CharMatcher.whitespace().matchesAllOf(byteRangeSet)) {
			throw new MalformedByteRangeException(); // empty string
		}
		List<String> byteRanges = Splitter.on(RANGE_SET_SEP).omitEmptyStrings().splitToList(byteRangeSet);
		if (byteRanges.size() == 1) {
			return byteRanges.get(0);
		} else {
			throw new UnsupportedRangeException(); // only a single range is expected
		}
	}

	private static ByteRange getPositions(String byteRangeStr) throws MalformedByteRangeException {
		final List<String> bytePos = Splitter.on(RANGE_SEP).splitToList(byteRangeStr);
		if (bytePos.size() != 2) {
			throw new MalformedByteRangeException();
		}
		try {
			Long first = bytePos.get(0).isEmpty() ? null : Long.valueOf(bytePos.get(0));
			Long last = bytePos.get(1).isEmpty() ? null : Long.valueOf(bytePos.get(1));
			return new ByteRange(first, last);
		} catch (NumberFormatException e) {
			throw new MalformedByteRangeException();
		}
	}

	/**
	 * @param contentLength Total size of the resource of which a range is requested.
	 * @return Index of first byte to be served bounded to <code>[0, contentLength)</code>
	 */
	public long getEffectiveFirstByte(long contentLength) {
		if (firstByte == null) {
			// bytes=-500
			assert lastByte != null;
			return Math.max(0, contentLength - lastByte);
		} else {
			// bytes=100-200
			// bytes=1000-
			return firstByte;
		}
	}

	/**
	 * @param contentLength Total size of the resource of which a range is requested.
	 * @return Index of last byte to be served bounded to <code>[0, contentLength)</code>
	 */
	public long getEffectiveLastByte(long contentLength) {
		if (firstByte == null || lastByte == null) {
			// bytes=-500
			// bytes=1000-
			return contentLength - 1;
		} else {
			// bytes=100-200
			return Math.min(lastByte, contentLength - 1);
		}
	}

	/**
	 * Indicates that a byte range is not understood or supported by this implementation.
	 * &quot;An origin server MUST ignore a Range header field that contains a range unit it does not understand.&quot;
	 * - <a href="https://tools.ietf.org/html/rfc7233#section-3.1">RFC 7233 Section 3.1</a>
	 */
	public static class UnsupportedRangeException extends Exception {
	}

	/**
	 * Indicates a malformed range header, which should be reported as client error.
	 */
	public static class MalformedByteRangeException extends Exception {
	}

}
