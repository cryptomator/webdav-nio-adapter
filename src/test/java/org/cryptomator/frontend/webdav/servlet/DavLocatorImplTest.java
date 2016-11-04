/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE.txt.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.frontend.webdav.servlet;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class DavLocatorImplTest {

	private DavLocatorFactoryImpl factory;
	private DavLocatorImpl locator;

	@Before
	public void setup() {
		factory = Mockito.mock(DavLocatorFactoryImpl.class);
		locator = new DavLocatorImpl(factory, "http://localhost/contextPath/", "foo/foo bar.txt");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConstructionWithInvalidPrefix() {
		new DavLocatorImpl(factory, "http://localhost/contextPath", "foo/foo bar.txt");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConstructionWithInvalidPath() {
		new DavLocatorImpl(factory, "http://localhost/contextPath/", "/foo/foo bar.txt");
	}

	@Test
	public void testGetResourcePath() {
		Assert.assertEquals("foo/foo bar.txt", locator.getResourcePath());
	}

	@Test
	public void testGetFactory() {
		Assert.assertSame(factory, locator.getFactory());
	}

}
