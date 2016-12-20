/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE.txt.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.frontend.webdav;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.cryptomator.frontend.webdav.WebDavServerModule.BindAddr;
import org.cryptomator.frontend.webdav.WebDavServerModule.CatchAll;
import org.cryptomator.frontend.webdav.WebDavServerModule.ServerPort;
import org.cryptomator.frontend.webdav.servlet.WebDavServletController;
import org.cryptomator.frontend.webdav.servlet.WebDavServletComponent;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The WebDAV server, that WebDAV servlets can be added to using {@link #startWebDavServlet(Path, String)}.
 * 
 * An instance of this class can be obtained via {@link #create(String, int)}.
 */
@Singleton
public class WebDavServer {

	private static final Logger LOG = LoggerFactory.getLogger(WebDavServer.class);

	private final Server server;
	private final ServerConnector localConnector;
	private final WebDavServletFactory servletFactory;

	@Inject
	WebDavServer(@ServerPort int port, @BindAddr String bindAddr, ContextHandlerCollection servletCollection, WebDavServletFactory servletContextFactory, @CatchAll ServletContextHandler catchAllServletHandler,
			ThreadPool threadPool) {
		this.server = new Server(threadPool);
		this.localConnector = new ServerConnector(server);
		this.servletFactory = servletContextFactory;
		localConnector.setHost(bindAddr);
		localConnector.setPort(port);
		servletCollection.addHandler(catchAllServletHandler);
		server.setConnectors(new Connector[] {localConnector});
		server.setHandler(servletCollection);
	}

	/**
	 * Creates a new WebDavServer listening on the given port. Ideally this method is invoked only once.
	 * 
	 * @param bindAddr Hostname or IP address, the WebDAV server's network interface should bind to. Use <code>0.0.0.0</code> to listen to all interfaces.
	 * @param port TCP port or <code>0</code> to use an auto-assigned port.
	 * @return A fully initialized but not yet running WebDavServer.
	 */
	public static WebDavServer create(String bindAddr, int port) {
		WebDavServerModule module = new WebDavServerModule(bindAddr, port);
		WebDavServerComponent comp = DaggerWebDavServerComponent.builder().webDavServerModule(module).build();
		return comp.server();
	}

	/**
	 * @return The TCP port this server is running on (if it's {@link #isRunning() running}. Otherwise {@link #start() start} it first).
	 */
	public int getPort() {
		return localConnector.getLocalPort();
	}

	/**
	 * @return <code>true</code> if the server is currently running.
	 */
	public boolean isRunning() {
		return server.isRunning();
	}

	/**
	 * Starts the WebDAV server.
	 * 
	 * @throws ServerLifecycleException If any exception occurs during server start (e.g. port not available).
	 */
	public synchronized void start() throws ServerLifecycleException {
		try {
			server.start();
			LOG.info("WebDavServer started on port {}.", getPort());
		} catch (Exception e) {
			throw new ServerLifecycleException("Server couldn't be started", e);
		}
	}

	/**
	 * Stops the WebDAV server.
	 * 
	 * @throws ServerLifecycleException If the server could not be stopped for any unexpected reason.
	 */
	public synchronized void stop() throws ServerLifecycleException {
		try {
			server.stop();
			LOG.info("WebDavServer stopped.");
		} catch (Exception e) {
			throw new ServerLifecycleException("Server couldn't be stopped", e);
		}
	}

	/**
	 * Creates a new WebDAV servlet (without starting it yet).
	 * 
	 * @param rootPath The path to the directory which should be served as root resource.
	 * @param contextPath The servlet context path, i.e. the path of the root resource.
	 * @return The controller object for this new servlet
	 */
	public WebDavServletController createWebDavServlet(Path rootPath, String contextPath) throws ServerLifecycleException {
		final URI uri = createUriForContextPath(contextPath);
		WebDavServletComponent servletComp = servletFactory.create(uri, rootPath);
		return servletComp.servlet();
	}

	private URI createUriForContextPath(String contextPath) {
		try {
			return new URI("http", null, "localhost", getPort(), StringUtils.prependIfMissing(contextPath, "/"), null, null);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Unable to construct valid URI for given contextPath.", e);
		}
	}

}
