/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE.txt.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.frontend.webdav;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.inject.Qualifier;
import javax.inject.Singleton;

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.ExecutorThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;

import dagger.Module;
import dagger.Provides;

@Module
class WebDavServerModule {

	private static final int MAX_PENDING_REQUESTS = 400;
	private static final int MAX_THREADS = 200;
	private static final int MIN_THREADS = 8;
	private static final int THREAD_IDLE_SECONDS = 10;
	private static final String ROOT_PATH = "/";

	private final int port;
	private final String bindAddr;

	/**
	 * @param bindAddr Hostname or IP address, the WebDAV server's network interface should bind to. Use <code>0.0.0.0</code> to listen to all interfaces.
	 * @param port TCP port or <code>0</code> to use an auto-assigned port.
	 */
	public WebDavServerModule(String bindAddr, int port) {
		this.bindAddr = Objects.requireNonNull(bindAddr);
		this.port = Objects.requireNonNull(port);
	}

	@Provides
	@ServerPort
	int providePort() {
		return port;
	}

	@Provides
	@BindAddr
	String provideBindAddr() {
		return bindAddr;
	}

	@Provides
	@Singleton
	ThreadPool serverThreadPool() {
		BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(MAX_PENDING_REQUESTS);
		return new ExecutorThreadPool(MIN_THREADS, MAX_THREADS, THREAD_IDLE_SECONDS, TimeUnit.SECONDS, queue);
	}

	@Provides
	@CatchAll
	ServletContextHandler createServletContextHandler(DefaultServlet servlet) {
		final ServletContextHandler servletContext = new ServletContextHandler(null, ROOT_PATH, ServletContextHandler.NO_SESSIONS);
		final ServletHolder servletHolder = new ServletHolder(ROOT_PATH, servlet);
		servletContext.addServlet(servletHolder, ROOT_PATH);
		return servletContext;
	}

	@Qualifier
	@Documented
	@Retention(RetentionPolicy.RUNTIME)
	@interface ServerPort {
	}

	@Qualifier
	@Documented
	@Retention(RetentionPolicy.RUNTIME)
	@interface BindAddr {
	}

	@Qualifier
	@Documented
	@Retention(RetentionPolicy.RUNTIME)
	@interface CatchAll {
	}

}
