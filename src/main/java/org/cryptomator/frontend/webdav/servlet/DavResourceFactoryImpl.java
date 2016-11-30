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
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavMethods;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletRequest;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.lock.LockManager;
import org.eclipse.jetty.http.HttpHeader;

@PerServlet
class DavResourceFactoryImpl implements DavResourceFactory {

	private static final String RANGE_BYTE_PREFIX = "bytes=";
	private static final char RANGE_SET_SEP = ',';
	private static final char RANGE_SEP = '-';

	private final Path rootPath;
	private final LockManager lockManager;

	@Inject
	public DavResourceFactoryImpl(@RootPath Path rootPath, ExclusiveSharedLockManager lockManager) {
		this.rootPath = rootPath;
		this.lockManager = lockManager;
	}

	@Override
	public DavResource createResource(DavResourceLocator locator, DavServletRequest request, DavServletResponse response) throws DavException {
		if (locator instanceof DavLocatorImpl && locator.equals(request.getRequestLocator())) {
			return createRequestResource((DavLocatorImpl) locator, request, response);
		} else if (locator instanceof DavLocatorImpl && locator.equals(request.getDestinationLocator())) {
			return createDestinationResource((DavLocatorImpl) locator, request, response);
		} else {
			throw new IllegalArgumentException("Unsupported locator of type " + locator.getClass());
		}
	}

	private DavResource createRequestResource(DavLocatorImpl locator, DavServletRequest request, DavServletResponse response) throws DavException {
		assert locator.equals(request.getRequestLocator());
		Path p = rootPath.resolve(locator.getResourcePath());
		Optional<BasicFileAttributes> attr = readBasicFileAttributes(p);
		if (!attr.isPresent() && DavMethods.METHOD_PUT.equals(request.getMethod())) {
			return createFile(locator, p, Optional.empty(), request.getDavSession());
		} else if (!attr.isPresent() && DavMethods.METHOD_MKCOL.equals(request.getMethod())) {
			return createFolder(locator, p, Optional.empty(), request.getDavSession());
		} else if (!attr.isPresent() && DavMethods.METHOD_LOCK.equals(request.getMethod())) {
			// locking non-existing resources must create a non-collection resource:
			// https://tools.ietf.org/html/rfc4918#section-9.10.4
			// See also: DavFile#lock(...)
			return createFile(locator, p, Optional.empty(), request.getDavSession());
		} else if (!attr.isPresent()) {
			throw new DavException(DavServletResponse.SC_NOT_FOUND);
		} else if (attr.get().isRegularFile() && DavMethods.METHOD_GET.equals(request.getMethod()) && request.getHeader(HttpHeader.RANGE.asString()) != null) {
			return createFileRange(locator, p, attr.get(), request.getDavSession(), request, response);
		} else if (attr.get().isRegularFile()) {
			return createFile(locator, p, attr, request.getDavSession());
		} else if (attr.get().isDirectory()) {
			return createFolder(locator, p, attr, request.getDavSession());
		} else {
			throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, "Resource is neither file nor directory.");
		}
	}

	private DavResource createDestinationResource(DavLocatorImpl locator, DavServletRequest request, DavServletResponse response) throws DavException {
		assert locator.equals(request.getDestinationLocator());
		assert ArrayUtils.contains(new String[] {DavMethods.METHOD_MOVE, DavMethods.METHOD_COPY}, request.getMethod());
		Path srcP = rootPath.resolve(request.getRequestLocator().getResourcePath());
		Path dstP = rootPath.resolve(locator.getResourcePath());
		Optional<BasicFileAttributes> srcAttr = readBasicFileAttributes(srcP);
		Optional<BasicFileAttributes> dstAttr = readBasicFileAttributes(dstP);
		if (!srcAttr.isPresent()) {
			throw new DavException(DavServletResponse.SC_NOT_FOUND);
		} else if (srcAttr.get().isRegularFile()) {
			return createFile(locator, dstP, dstAttr, request.getDavSession());
		} else if (srcAttr.get().isDirectory()) {
			return createFolder(locator, dstP, dstAttr, request.getDavSession());
		} else {
			throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, "Resource is neither file nor directory.");
		}
	}

	@Override
	public DavResource createResource(DavResourceLocator locator, DavSession session) throws DavException {
		if (locator instanceof DavLocatorImpl) {
			return createResourceInternal((DavLocatorImpl) locator, session);
		} else {
			throw new IllegalArgumentException("Unsupported locator of type " + locator.getClass());
		}
	}

	private DavResource createResourceInternal(DavLocatorImpl locator, DavSession session) throws DavException {
		Path p = rootPath.resolve(locator.getResourcePath());
		Optional<BasicFileAttributes> attr = readBasicFileAttributes(p);
		if (!attr.isPresent()) {
			throw new DavException(DavServletResponse.SC_NOT_FOUND);
		} else if (attr.get().isRegularFile()) {
			return createFile(locator, p, attr, session);
		} else if (attr.get().isDirectory()) {
			return createFolder(locator, p, attr, session);
		} else {
			throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, "Resource is neither file nor directory.");
		}
	}

	/**
	 * @return BasicFileAttributes or {@link Optional#empty()} if the file/folder for the given path does not exist.
	 * @throws DavException If an {@link IOException} occured during {@link Files#readAttributes(Path, Class, java.nio.file.LinkOption...)}.
	 */
	private Optional<BasicFileAttributes> readBasicFileAttributes(Path path) throws DavException {
		try {
			return Optional.of(Files.readAttributes(path, BasicFileAttributes.class));
		} catch (NoSuchFileException e) {
			return Optional.empty();
		} catch (IOException e) {
			throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, e);
		}
	}

	DavFolder createFolder(DavLocatorImpl locator, Path path, Optional<BasicFileAttributes> attr, DavSession session) {
		return new DavFolder(this, lockManager, locator, path, attr, session);
	}

	DavFile createFile(DavLocatorImpl locator, Path path, Optional<BasicFileAttributes> attr, DavSession session) {
		return new DavFile(this, lockManager, locator, path, attr, session);
	}

	private DavFile createFileRange(DavLocatorImpl locator, Path path, BasicFileAttributes attr, DavSession session, DavServletRequest request, DavServletResponse response) throws DavException {
		// 200 for "normal" resources, if if-range is not satisified:
		final String ifRangeHeader = request.getHeader(HttpHeader.IF_RANGE.asString());
		if (!isIfRangeHeaderSatisfied(attr, ifRangeHeader)) {
			return createFile(locator, path, Optional.of(attr), session);
		}

		final String rangeHeader = request.getHeader(HttpHeader.RANGE.asString());
		try {
			// 206 for ranged resources:
			final Pair<String, String> parsedRange = parseRangeRequestHeader(rangeHeader);
			response.setStatus(DavServletResponse.SC_PARTIAL_CONTENT);
			return new DavFileWithRange(this, lockManager, locator, path, attr, session, parsedRange);
		} catch (DavException ex) {
			if (ex.getErrorCode() == DavServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE) {
				// 416 for unsatisfiable ranges:
				response.setStatus(DavServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
				return new DavFileWithUnsatisfiableRange(this, lockManager, locator, path, attr, session);
			} else {
				throw new DavException(ex.getErrorCode(), ex);
			}
		}
	}

	/**
	 * Processes the given range header field, if it is supported. Only headers containing a single byte range are supported.<br/>
	 * <code>
	 * bytes=100-200<br/>
	 * bytes=-500<br/>
	 * bytes=1000-
	 * </code>
	 * 
	 * @return Tuple of lower and upper range.
	 * @throws DavException HTTP statuscode 400 for malformed requests. 416 if requested range is not supported.
	 */
	private Pair<String, String> parseRangeRequestHeader(String rangeHeader) throws DavException {
		assert rangeHeader != null;
		if (!rangeHeader.startsWith(RANGE_BYTE_PREFIX)) {
			throw new DavException(DavServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
		}
		final String byteRangeSet = StringUtils.removeStartIgnoreCase(rangeHeader, RANGE_BYTE_PREFIX);
		final String[] byteRanges = StringUtils.split(byteRangeSet, RANGE_SET_SEP);
		if (byteRanges.length != 1) {
			throw new DavException(DavServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
		}
		final String byteRange = byteRanges[0];
		final String[] bytePos = StringUtils.splitPreserveAllTokens(byteRange, RANGE_SEP);
		if (bytePos.length != 2 || bytePos[0].isEmpty() && bytePos[1].isEmpty()) {
			throw new DavException(DavServletResponse.SC_BAD_REQUEST, "malformed range header: " + rangeHeader);
		}
		return new ImmutablePair<>(bytePos[0], bytePos[1]);
	}

	/**
	 * @return <code>true</code> if a partial response should be generated according to an If-Range precondition.
	 */
	private boolean isIfRangeHeaderSatisfied(BasicFileAttributes attr, String ifRangeHeader) throws DavException {
		if (ifRangeHeader == null) {
			// no header set -> satisfied implicitly
			return true;
		} else {
			try {
				Instant expectedTime = Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(ifRangeHeader));
				Instant actualTime = attr.lastModifiedTime().toInstant();
				return expectedTime.compareTo(actualTime) == 0;
			} catch (DateTimeParseException e) {
				throw new DavException(DavServletResponse.SC_BAD_REQUEST, "Unsupported If-Range header: " + ifRangeHeader);
			}
		}
	}

}
