/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE.txt.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.frontend.webdav;

import static java.lang.String.format;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.cryptomator.frontend.webdav.WebDavServerModule.ServerPort;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class WebDavServer {

	private static final Logger LOG = LoggerFactory.getLogger(WebDavServer.class);
	private static final int MAX_PENDING_REQUESTS = 200;
	private static final int MAX_THREADS = 200;
	private static final int MIN_THREADS = 4;
	private static final int THREAD_IDLE_SECONDS = 20;

	private final Server server;
	private final ServerConnector localConnector;
	private final ContextHandlerCollection servletCollection;
	private final WebDavServletContextFactory servletContextFactory;

	@Inject
	WebDavServer(@ServerPort int port, WebDavServletContextFactory servletContextFactory, DefaultServlet defaultServlet) {
		final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(MAX_PENDING_REQUESTS);
		final ThreadPool tp = new QueuedThreadPool(MAX_THREADS, MIN_THREADS, THREAD_IDLE_SECONDS, queue);
		this.server = new Server(tp);
		this.localConnector = new ServerConnector(server);
		this.servletCollection = new ContextHandlerCollection();
		this.servletContextFactory = servletContextFactory;
		this.localConnector.setPort(port);
		servletCollection.addHandler(defaultServlet.createServletContextHandler());
		server.setConnectors(new Connector[] {localConnector});
		server.setHandler(servletCollection);
	}

	public int getPort() {
		return localConnector.getLocalPort();
	}

	public synchronized void start() {
		try {
			server.start();
			LOG.info("Cryptomator is running on port {}", getPort());
		} catch (Exception ex) {
			throw new RuntimeException("Server couldn't be started", ex);
		}
	}

	public boolean isRunning() {
		return server.isRunning();
	}

	public synchronized void stop() {
		try {
			server.stop();
		} catch (Exception ex) {
			LOG.error("Server couldn't be stopped", ex);
		}
	}

	private ServletContextHandler prepareWebDavServlet(URI contextRoot, Path rootPath) {
		ServletContextHandler handler = servletContextFactory.create(contextRoot, rootPath);
		servletCollection.addHandler(handler);
		servletCollection.mapContexts();
		return handler;
	}

	public void create(Path rootPath, String name) {
		String contextPath = format("/%s", name);
		final URI uri;
		try {
			uri = new URI("http", null, "localhost", getPort(), contextPath, null, null);
		} catch (URISyntaxException e) {
			throw new IllegalStateException(e);
		}
		final ServletContextHandler handler = prepareWebDavServlet(uri, rootPath);
		try {
			handler.start();
			LOG.info("Servlet available under " + uri);
		} catch (Exception e) {
			LOG.error("Servlet could not be started.", e);
		}
	}

}
