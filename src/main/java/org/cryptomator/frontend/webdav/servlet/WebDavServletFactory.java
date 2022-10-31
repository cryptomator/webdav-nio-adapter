/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE.txt.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.frontend.webdav.servlet;

import com.google.common.base.CharMatcher;
import org.cryptomator.frontend.webdav.mount.LegacyMounter;
import org.cryptomator.webdav.core.filters.*;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import javax.servlet.DispatcherType;
import javax.servlet.Servlet;
import java.nio.file.Path;
import java.util.EnumSet;

public class WebDavServletFactory {

	private WebDavServletFactory(){}

	private static final String WILDCARD = "/*";

	public static ServletContextHandler createServletContext(Path rootPath, String contextPath) {
		final Servlet servlet = new FixedPathNioWebDavServlet(rootPath);
		final ServletContextHandler servletContext = new ServletContextHandler(null, contextPath, ServletContextHandler.SESSIONS);
		final ServletHolder servletHolder = new ServletHolder(contextPath, servlet);
		servletContext.addServlet(servletHolder, WILDCARD);
		servletContext.addFilter(LoggingFilter.class, WILDCARD, EnumSet.of(DispatcherType.REQUEST));
		servletContext.addFilter(UnicodeResourcePathNormalizationFilter.class, WILDCARD, EnumSet.of(DispatcherType.REQUEST));
		servletContext.addFilter(PostRequestBlockingFilter.class, WILDCARD, EnumSet.of(DispatcherType.REQUEST));
		servletContext.addFilter(MkcolComplianceFilter.class, WILDCARD, EnumSet.of(DispatcherType.REQUEST));
		servletContext.addFilter(AcceptRangeFilter.class, WILDCARD, EnumSet.of(DispatcherType.REQUEST));
		servletContext.addFilter(MacChunkedPutCompatibilityFilter.class, WILDCARD, EnumSet.of(DispatcherType.REQUEST));
		return servletContext;
	}

	public static WebDavServletController createServletController(Path rootPath, String untrimmedContextPath, ServerConnector serverConnector, ContextHandlerCollection contextHandlerCollection) {
		String trimmedCtxPath = CharMatcher.is('/').trimTrailingFrom(untrimmedContextPath);
		String contextPath = trimmedCtxPath.startsWith("/") ? trimmedCtxPath : "/" + trimmedCtxPath;
		ServletContextHandler contextHandler = createServletContext(rootPath, contextPath);
		LegacyMounter mounter = LegacyMounter.find(); // TODO remove
		return new WebDavServletController(contextHandler, contextHandlerCollection, serverConnector, contextPath, mounter);
	}

}
