/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE.txt.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.frontend.webdav.servlet;

import java.util.Objects;

import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.util.EncodeUtil;

import com.google.common.base.CharMatcher;

class DavLocatorImpl implements DavResourceLocator {

	private final DavLocatorFactoryImpl factory;
	private final String prefix;
	private final String resourcePath;

	/**
	 * Behold, this is a constructor. It constructs constructions. Didn't see that coming, did you?
	 * 
	 * @param factory Locator factory.
	 * @param prefix Must end on "/".
	 * @param resourcePath Must be an relative path, i.e. must not start with "/".
	 */
	public DavLocatorImpl(DavLocatorFactoryImpl factory, String prefix, String resourcePath) {
		if (!prefix.endsWith("/")) {
			throw new IllegalArgumentException("prefix must end on '/' but was: " + prefix);
		}
		if (resourcePath.startsWith("/")) {
			throw new IllegalArgumentException("resourcePath must not start with '/' but was: " + resourcePath);
		}
		this.factory = Objects.requireNonNull(factory);
		this.prefix = prefix;
		this.resourcePath = CharMatcher.is('/').trimTrailingFrom(resourcePath);
	}

	public DavLocatorImpl resolveChild(String childName) {
		if (isRootLocation()) {
			return factory.createResourceLocator(prefix, null, childName);
		} else {
			return factory.createResourceLocator(prefix, null, resourcePath + "/" + childName);
		}
	}

	public DavLocatorImpl resolveParent() {
		if (isRootLocation()) {
			// root does not have a parent:
			return null;
		} else if (resourcePath.contains("/")) {
			// parent is a directory:
			String parentResourcePath = resourcePath.substring(0, CharMatcher.is('/').lastIndexIn(resourcePath));
			return factory.createResourceLocator(prefix, null, parentResourcePath);
		} else {
			// parent is root:
			return factory.createResourceLocator(prefix, null, "");
		}
	}

	@Override
	public String getPrefix() {
		return prefix;
	}

	@Override
	public String getResourcePath() {
		return resourcePath;
	}

	@Override
	public String getWorkspacePath() {
		// TODO overheadhunter: what defines a workspace? same servlet?
		return null;
	}

	@Override
	public String getWorkspaceName() {
		// TODO overheadhunter: what defines a workspace? same servlet?
		return null;
	}

	@Override
	public boolean isSameWorkspace(DavResourceLocator locator) {
		// TODO overheadhunter: what defines a workspace? same servlet?
		return false;
	}

	@Override
	public boolean isSameWorkspace(String workspaceName) {
		// TODO overheadhunter: what defines a workspace? same servlet?
		return false;
	}

	@Override
	public String getHref(boolean isCollection) {
		String href = getHref();
		if (isCollection) {
			return href.endsWith("/") ? href : href + "/";
		} else {
			return CharMatcher.is('/').trimTrailingFrom(href);
		}
	}

	private String getHref() {
		return prefix + EncodeUtil.escapePath(resourcePath);
	}

	@Override
	public boolean isRootLocation() {
		return resourcePath.isEmpty();
	}

	@Override
	public DavLocatorFactoryImpl getFactory() {
		return factory;
	}

	@Override
	public String getRepositoryPath() {
		return getResourcePath();
	}

	@Override
	public int hashCode() {
		return Objects.hash(factory, prefix, resourcePath);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DavLocatorImpl) {
			DavLocatorImpl other = (DavLocatorImpl) obj;
			assert this.factory != null //
					&& this.prefix != null //
					&& this.resourcePath != null;
			return this.factory.equals(other.factory) //
					&& this.prefix.equals(other.prefix) //
					&& this.resourcePath.equals(other.resourcePath);
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		return this.factory + ": " + this.prefix + this.resourcePath;
	}

}
