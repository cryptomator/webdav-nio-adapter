/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE.txt.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.frontend.webdav.servlet;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URI;
import java.nio.file.Path;
import java.util.EnumSet;

import javax.inject.Qualifier;
import javax.inject.Scope;
import javax.servlet.DispatcherType;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import dagger.Module;
import dagger.Provides;

@Module
public class WebDavServletModule {

	private static final String WILDCARD = "/*";

	private final URI contextRoot;
	private final Path rootPath;

	public WebDavServletModule(URI contextRoot, Path rootPath) {
		this.contextRoot = contextRoot;
		this.rootPath = rootPath;
	}

	@PerServlet
	@Provides
	@ContextRoot
	public URI provideContextRootUri() {
		return contextRoot;
	}

	@PerServlet
	@Provides
	@RootPath
	public Path provideRootPath() {
		return rootPath;
	}

	// @PerServlet
	// @Provides
	// @Named("loopbackFilter")
	// public FilterHolder provideLoopbackFilter() {
	// return new FilterHolder(new LoopbackFilter());
	// }
	//
	// @PerServlet
	// @Provides
	// @Named("postRequestBlockingFilter")
	// public FilterHolder providePostRequestBlockingFilter() {
	// return new FilterHolder(new PostRequestBlockingFilter());
	// }
	//
	// @PerServlet
	// @Provides
	// @Named("mkcolComplianceFilter")
	// public FilterHolder provideMkColComplianceFilter() {
	// return new FilterHolder(new MkcolComplianceFilter());
	// }
	//
	// @PerServlet
	// @Provides
	// @Named("acceptRangeFilter")
	// public FilterHolder provideAcceptRangeFilter() {
	// return new FilterHolder(new AcceptRangeFilter());
	// }
	//
	// @PerServlet
	// @Provides
	// @Named("macChunkedPutCompatibilityFilter")
	// public FilterHolder provideMacChunkedPutCompatibilityFilter() {
	// return new FilterHolder(new MacChunkedPutCompatibilityFilter());
	// }

	@PerServlet
	@Provides
	public ServletContextHandler provideServletContext(WebDavServlet servlet) {
		final String contextPath = StringUtils.removeEnd(contextRoot.getPath(), "/");
		final ServletContextHandler servletContext = new ServletContextHandler(null, contextPath, ServletContextHandler.SESSIONS);
		final ServletHolder servletHolder = new ServletHolder(contextPath, servlet);
		servletContext.addServlet(servletHolder, WILDCARD);
		servletContext.addFilter(PostRequestBlockingFilter.class, WILDCARD, EnumSet.of(DispatcherType.REQUEST));
		servletContext.addFilter(MkcolComplianceFilter.class, WILDCARD, EnumSet.of(DispatcherType.REQUEST));
		servletContext.addFilter(AcceptRangeFilter.class, WILDCARD, EnumSet.of(DispatcherType.REQUEST));
		servletContext.addFilter(MacChunkedPutCompatibilityFilter.class, WILDCARD, EnumSet.of(DispatcherType.REQUEST));
		return servletContext;
	}

	@Qualifier
	@Documented
	@Retention(RetentionPolicy.RUNTIME)
	@interface ContextRoot {
	}

	@Qualifier
	@Documented
	@Retention(RetentionPolicy.RUNTIME)
	@interface RootPath {
	}

	@Scope
	@Documented
	@Retention(RetentionPolicy.RUNTIME)
	@interface PerServlet {
	}

}
