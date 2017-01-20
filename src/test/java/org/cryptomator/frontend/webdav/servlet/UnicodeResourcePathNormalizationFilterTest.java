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

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class UnicodeResourcePathNormalizationFilterTest {

	private UnicodeResourcePathNormalizationFilter filter;
	private FilterChain chain;
	private HttpServletRequest request;
	private HttpServletResponse response;

	@Before
	public void setup() {
		filter = new UnicodeResourcePathNormalizationFilter();
		chain = Mockito.mock(FilterChain.class);
		request = Mockito.mock(HttpServletRequest.class);
		response = Mockito.mock(HttpServletResponse.class);

		Mockito.when(request.getScheme()).thenReturn("http");
		Mockito.when(request.getServerName()).thenReturn("example.com");
		Mockito.when(request.getServerPort()).thenReturn(80);
		Mockito.when(request.getContextPath()).thenReturn("/foo");
	}

	@Test
	public void testRequestWithNormalizedResourceUri() throws IOException, ServletException {
		Mockito.when(request.getPathInfo()).thenReturn("/bar");
		filter.doFilter(request, response, chain);

		ArgumentCaptor<HttpServletRequest> wrappedReq = ArgumentCaptor.forClass(HttpServletRequest.class);
		Mockito.verify(chain).doFilter(wrappedReq.capture(), Mockito.any(ServletResponse.class));
		Mockito.verify(request, Mockito.never()).getRequestURI();
		Assert.assertEquals("/foo/bar", wrappedReq.getValue().getRequestURI());
		Assert.assertEquals("http://example.com/foo/bar", wrappedReq.getValue().getRequestURL().toString());
	}

	@Test
	public void testRequestWithNonNormalizedResourceUri() throws IOException, ServletException {
		Mockito.when(request.getPathInfo()).thenReturn("/\u0041\u030A");
		filter.doFilter(request, response, chain);

		ArgumentCaptor<HttpServletRequest> wrappedReq = ArgumentCaptor.forClass(HttpServletRequest.class);
		Mockito.verify(chain).doFilter(wrappedReq.capture(), Mockito.any(ServletResponse.class));
		Mockito.verify(request, Mockito.never()).getRequestURI();
		Assert.assertEquals("/\u00C5", wrappedReq.getValue().getPathInfo());
		Assert.assertEquals("/foo/\u00C5", wrappedReq.getValue().getRequestURI());
		Assert.assertEquals("http://example.com/foo/\u00C5", wrappedReq.getValue().getRequestURL().toString());
	}

	@Test
	public void testRequestWithNonNormalizedDestinationUri() throws IOException, ServletException {
		Mockito.when(request.getHeader("Destination")).thenReturn("http://example.com/bar/\u0041\u030A");
		filter.doFilter(request, response, chain);

		ArgumentCaptor<HttpServletRequest> wrappedReq = ArgumentCaptor.forClass(HttpServletRequest.class);
		Mockito.verify(chain).doFilter(wrappedReq.capture(), Mockito.any(ServletResponse.class));
		Assert.assertEquals("http://example.com/bar/\u00C5", wrappedReq.getValue().getHeader("Destination"));
	}

	@Test
	public void foo() throws IOException, ServletException {
		Mockito.when(request.getPathInfo()).thenReturn("/O\u00CC\u0088");

		filter.doFilter(request, response, chain);

		ArgumentCaptor<HttpServletRequest> wrappedReq = ArgumentCaptor.forClass(HttpServletRequest.class);
		Mockito.verify(chain).doFilter(wrappedReq.capture(), Mockito.any(ServletResponse.class));
		Assert.assertEquals("http://example.com/foo/Ö", wrappedReq.getValue().getRequestURL().toString());
	}

}
