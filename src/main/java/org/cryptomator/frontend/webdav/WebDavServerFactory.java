/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE.txt.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.frontend.webdav;

import org.eclipse.jetty.http.UriCompliance;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.ExecutorThreadPool;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class WebDavServerFactory {

	private static final int MAX_PENDING_REQUESTS = 400;
	private static final int MAX_THREADS = 100;
	private static final int THREAD_IDLE_SECONDS = 60;
	private static final String ROOT_PATH = "/";
	private static final AtomicInteger THREAD_NUM = new AtomicInteger();

	private WebDavServerFactory(){}

	private static ThreadPoolExecutor createThreadPoolExecutor() {
		// set core pool size = MAX_THREADS and allow coreThreadTimeOut to enforce spawning threads till the maximum even if the queue is not full
		BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(MAX_PENDING_REQUESTS);
		ThreadPoolExecutor executor = new ThreadPoolExecutor(MAX_THREADS, MAX_THREADS, THREAD_IDLE_SECONDS, TimeUnit.SECONDS, queue);
		executor.allowCoreThreadTimeOut(true);
		return executor;
	}

	private static ExecutorThreadPool createThreadPool(ThreadPoolExecutor executorService) {
		ExecutorThreadPool threadPool = new ExecutorThreadPool(executorService);
		executorService.setThreadFactory(WebDavServerFactory::createServerThread);
		try {
			threadPool.start();
			return threadPool;
		} catch (Exception e) {
			throw new IllegalStateException("Implementation known not to throw an exception.", e);
		}
	}

	private static Thread createServerThread(Runnable runnable) {
		Thread t = new Thread(runnable, String.format("webdav-%03d", THREAD_NUM.incrementAndGet()));
		t.setDaemon(true);
		return t;
	}


	private static Server createServer(ExecutorThreadPool threadPool, ContextHandlerCollection servletCollection) {
		if (!threadPool.isStarted()) {
			// otherwise addBean() will make the threadpool managed, i.e. it will be shut down when the server is stopped
			throw new IllegalStateException();
		}
		Server server = new Server(threadPool);
		server.setHandler(servletCollection);
		return server;
	}

	private static ServerConnector createServerConnector(Server server, InetSocketAddress bindAddr) {
		HttpConfiguration config = new HttpConfiguration();
		config.setUriCompliance(UriCompliance.from("0,AMBIGUOUS_PATH_SEPARATOR,AMBIGUOUS_PATH_ENCODING"));
		ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory(config));
		connector.setHost(bindAddr.getHostString());
		connector.setPort(bindAddr.getPort());
		server.setConnectors(new Connector[]{connector});
		return connector;
	}

	private static ContextHandlerCollection createContextHandlerCollection(ServletContextHandler catchAllServletHandler) {
		ContextHandlerCollection collection = new ContextHandlerCollection();
		collection.addHandler(catchAllServletHandler);
		return collection;
	}

	private static ServletContextHandler createDefaultServletContext(DefaultServlet servlet) {
		final ServletContextHandler servletContext = new ServletContextHandler(null, ROOT_PATH, ServletContextHandler.NO_SESSIONS);
		final ServletHolder servletHolder = new ServletHolder(ROOT_PATH, servlet);
		servletContext.addServlet(servletHolder, ROOT_PATH);
		return servletContext;
	}

	public static WebDavServer createWebDavServer(InetSocketAddress bindAddr) {
		var contextPaths = new HashSet<String>();
		var executorService = createThreadPoolExecutor();
		var threadPool = createThreadPool(executorService);
		var defaultServlet = new DefaultServlet(contextPaths);
		var defaultServletCtx = createDefaultServletContext(defaultServlet);
		var servletCollectionCtx = createContextHandlerCollection(defaultServletCtx);
		var server = createServer(threadPool, servletCollectionCtx);
		var serverConnector = createServerConnector(server, bindAddr);
		return new WebDavServer(server, executorService, serverConnector, servletCollectionCtx);
	}

}
