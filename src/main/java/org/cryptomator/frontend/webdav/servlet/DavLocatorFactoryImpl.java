/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE.txt.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.frontend.webdav.servlet;

import javax.inject.Inject;

import org.apache.jackrabbit.webdav.DavLocatorFactory;
import org.apache.jackrabbit.webdav.util.EncodeUtil;
import org.cryptomator.frontend.webdav.servlet.WebDavServletModule.PerServlet;

@PerServlet
class DavLocatorFactoryImpl implements DavLocatorFactory {

	@Inject
	public DavLocatorFactoryImpl() {
	}

	@Override
	public DavLocatorImpl createResourceLocator(String prefix, String href) {
		final String canonicalPrefix = prefix.endsWith("/") ? prefix : prefix + "/";
		final String canonicalHref = href.startsWith("/") ? href.substring(1) : href;
		final String hrefWithoutPrefix = canonicalHref.startsWith(canonicalPrefix) ? canonicalHref.substring(canonicalPrefix.length()) : canonicalHref;
		final String resourcePath = EncodeUtil.unescape(hrefWithoutPrefix);
		return createResourceLocator(canonicalPrefix, null, resourcePath);
	}

	@Override
	public DavLocatorImpl createResourceLocator(String prefix, String workspacePath, String resourcePath) {
		return new DavLocatorImpl(this, prefix, resourcePath);
	}

	@Override
	public DavLocatorImpl createResourceLocator(String prefix, String workspacePath, String path, boolean isResourcePath) {
		// ignore isResourcePath. This impl doesn't distinguish resourcePath and repositoryPath.
		return createResourceLocator(prefix, workspacePath, path);
	}

}
