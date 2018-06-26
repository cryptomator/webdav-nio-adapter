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
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Qualifier;
import javax.inject.Singleton;

import org.cryptomator.frontend.webdav.mount.MounterModule;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.ExecutorSizedThreadPool;
import org.eclipse.jetty.util.thread.ExecutorThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;

import dagger.Module;
import dagger.Provides;

@Module(includes = {MounterModule.class})
class WebDavServerModule {

	private static final int MAX_PENDING_REQUESTS = 400;
	private static final int MAX_THREADS = 100;
	private static final int THREAD_IDLE_SECONDS = 60;
	private static final String ROOT_PATH = "/";

	@Provides
	@Singleton
	ThreadPoolExecutor provideThreadPoolExecutor() {
		// set core pool size = MAX_THREADS and allow coreThreadTimeOut to enforce spawning threads till the maximum even if the queue is not full
		BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(MAX_PENDING_REQUESTS);
		AtomicInteger threadNum = new AtomicInteger(1);
		ThreadPoolExecutor executor = new ThreadPoolExecutor(MAX_THREADS, MAX_THREADS, THREAD_IDLE_SECONDS, TimeUnit.SECONDS, queue, r -> {
			Thread t = new Thread(r, String.format("Server thread %03d", threadNum.getAndIncrement()));
			t.setDaemon(true);
			return t;
		});
		executor.allowCoreThreadTimeOut(true);
		return executor;
	}

	@Provides
	@Singleton
	ExecutorService provideExecutorService(ThreadPoolExecutor executorService) {
		return executorService;
	}

	@Provides
	@Singleton
	Server provideServer(ThreadPoolExecutor executorService, ContextHandlerCollection servletCollection) {
		ThreadPool threadPool = new ExecutorThreadPool(executorService);
		Server server = new Server(threadPool);
		server.unmanage(threadPool); // prevent threadpool from being shutdown when stopping the server
		server.setHandler(servletCollection);
		return server;
	}

	@Provides
	@Singleton
	ServerConnector provideServerConnector(Server server) {
		ServerConnector connector = new ServerConnector(server);
		server.setConnectors(new Connector[] {connector});
		return connector;
	}

	@Provides
	@Singleton
	ContextHandlerCollection provideContextHandlerCollection(@CatchAll ServletContextHandler catchAllServletHandler) {
		ContextHandlerCollection collection = new ContextHandlerCollection();
		collection.addHandler(catchAllServletHandler);
		return collection;
	}

	@Provides
	@Singleton
	@CatchAll
	ServletContextHandler provideServletContextHandler(DefaultServlet servlet) {
		final ServletContextHandler servletContext = new ServletContextHandler(null, ROOT_PATH, ServletContextHandler.NO_SESSIONS);
		final ServletHolder servletHolder = new ServletHolder(ROOT_PATH, servlet);
		servletContext.addServlet(servletHolder, ROOT_PATH);
		return servletContext;
	}

	@Provides
	@Singleton
	@ContextPaths
	Collection<String> provideContextPaths() {
		return new HashSet<>();
	}

	@Qualifier
	@Documented
	@Retention(RetentionPolicy.RUNTIME)
	@interface CatchAll {
	}

	@Qualifier
	@Documented
	@Retention(RetentionPolicy.RUNTIME)
	@interface ContextPaths {
	}

}
