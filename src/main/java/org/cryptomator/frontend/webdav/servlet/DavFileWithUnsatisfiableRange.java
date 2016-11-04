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
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.io.OutputContext;
import org.apache.jackrabbit.webdav.lock.LockManager;

/**
 * Sends the full file in reaction to an unsatisfiable range.
 * 
 * @see {@link https://tools.ietf.org/html/rfc7233#section-4.2}
 */
class DavFileWithUnsatisfiableRange extends DavFile {

	private static final String CONTENT_RANGE_HEADER = "Content-Disposition";

	public DavFileWithUnsatisfiableRange(DavResourceFactoryImpl factory, LockManager lockManager, DavLocatorImpl locator, Path path, BasicFileAttributes attr, DavSession session) throws DavException {
		super(factory, lockManager, locator, path, Optional.of(attr), session);
	}

	@Override
	public void spool(OutputContext outputContext) throws IOException {
		assert exists();
		outputContext.setProperty(CONTENT_RANGE_HEADER, "bytes */" + attr.get().size());
		super.spool(outputContext);
	}

}
