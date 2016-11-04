/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE.txt.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.frontend.webdav.servlet;

import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.junit.Assert;
import org.junit.Test;

public class DavLocatorFactoryImplTest {

	@Test
	public void testCreateResourceLocatorFromHref() {
		DavLocatorFactoryImpl factory = new DavLocatorFactoryImpl();
		DavResourceLocator loc1 = factory.createResourceLocator("http://localhost:123/contextPath/", "http://localhost:123/contextPath/foo/foo%20bar.txt");
		Assert.assertEquals("foo/foo bar.txt", loc1.getResourcePath());

		DavResourceLocator loc2 = factory.createResourceLocator("http://localhost:123/contextPath/", "relative/path.txt");
		Assert.assertEquals("relative/path.txt", loc2.getResourcePath());
	}

}
