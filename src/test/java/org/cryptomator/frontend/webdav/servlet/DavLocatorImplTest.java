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

	@Test
	public void testConstructionWithTrailingSlash() {
		DavLocatorImpl locator = new DavLocatorImpl(factory, "http://localhost/contextPath/", "foo/bar/baz/");
		Assert.assertEquals("foo/bar/baz", locator.getResourcePath());
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
	public void testResolveParent1() {
		DavLocatorImpl fooBarBaz = new DavLocatorImpl(factory, "http://localhost/contextPath/", "foo/bar/baz");
		DavLocatorImpl fooBar = new DavLocatorImpl(factory, "http://localhost/contextPath/", "foo/bar");
		Mockito.when(factory.createResourceLocator("http://localhost/contextPath/", null, "foo/bar")).thenReturn(fooBar);
		DavLocatorImpl result = fooBarBaz.resolveParent();
		Assert.assertEquals(fooBar, result);
	}

	@Test
	public void testResolveParent2() {
		DavLocatorImpl foo = new DavLocatorImpl(factory, "http://localhost/contextPath/", "foo");
		DavLocatorImpl root = new DavLocatorImpl(factory, "http://localhost/contextPath/", "");
		Mockito.when(factory.createResourceLocator("http://localhost/contextPath/", null, "")).thenReturn(root);
		DavLocatorImpl result = foo.resolveParent();
		Assert.assertEquals(root, result);
	}

	@Test
	public void testResolveParent3() {
		DavLocatorImpl root = new DavLocatorImpl(factory, "http://localhost/contextPath/", "");
		Assert.assertNull(root.resolveParent());
	}

	@Test
	public void testGetFactory() {
		Assert.assertSame(factory, locator.getFactory());
	}

}
