/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE.txt.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.frontend.webdav;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.cryptomator.frontend.webdav.servlet.WebDavServletComponent;
import org.cryptomator.frontend.webdav.servlet.WebDavServletController;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The WebDAV server, that WebDAV servlets can be added to using {@link #createWebDavServlet(Path, String)}.
 *
 * An instance of this class can be obtained via {@link #create()}.
 */
@Singleton
public class WebDavServer {

	private static final Logger LOG = LoggerFactory.getLogger(WebDavServer.class);

	private final Server server;
	private final ExecutorService executorService;
	private final ServerConnector localConnector;
	private final WebDavServletFactory servletFactory;

	@Inject
	WebDavServer(Server server, ExecutorService executorService, ServerConnector connector, WebDavServletFactory servletContextFactory) {
		this.server = server;
		this.executorService = executorService;
		this.localConnector = connector;
		this.servletFactory = servletContextFactory;
	}

	public static WebDavServer create() {
		WebDavServerComponent comp = DaggerWebDavServerComponent.create();
		return comp.server();
	}

	/**
	 * Reconfigures the server socket to listen on the specified bindAddr and port.
	 *
	 * @param bindAddr Hostname or IP address, the WebDAV server's network interface should bind to. Use <code>0.0.0.0</code> to listen to all interfaces.
	 * @param port TCP port or <code>0</code> to use an auto-assigned port.
	 * @throws ServerLifecycleException If any exception occurs during socket reconfiguration (e.g. port not available).
	 */
	public void bind(String bindAddr, int port) {
		this.bind(InetSocketAddress.createUnresolved(bindAddr, port));
	}

	/**
	 * Reconfigures the server socket to listen on the specified bindAddr and port.
	 *
	 * @param socketBindAddress Socket address and port of the server. Use <code>0.0.0.0:0</code> to listen on all interfaces and auto-assign a port.
	 * @throws ServerLifecycleException If any exception occurs during socket reconfiguration (e.g. port not available).
	 */
	public void bind(InetSocketAddress socketBindAddress) {
		try {
			localConnector.stop();
			LOG.info("Binding server socket to {}:{}", socketBindAddress.getHostString(), socketBindAddress.getPort());
			localConnector.setHost(socketBindAddress.getHostString());
			localConnector.setPort(socketBindAddress.getPort());
			localConnector.start();
		} catch (Exception e) {
			throw new ServerLifecycleException("Failed to restart socket.", e);
		}
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
		if (executorService.isShutdown()) {
			throw new IllegalStateException("Server has already been terminated.");
		}
		try {
			server.start();
			LOG.info("WebDavServer started.");
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
	 * Stops the WebDAV server and shuts down its executor service. After terminating, this instance can no longer be restarted.
	 * 
	 * @throws ServerLifecycleException If the server could not be stopped for any unexpected reason.
	 */
	public synchronized void terminate() throws ServerLifecycleException {
		stop();
		executorService.shutdownNow();
	}

	/**
	 * Creates a new WebDAV servlet (without starting it yet).
	 *
	 * @param rootPath The path to the directory which should be served as root resource.
	 * @param contextPath The servlet context path, i.e. the path of the root resource.
	 * @return The controller object for this new servlet
	 */
	public WebDavServletController createWebDavServlet(Path rootPath, String contextPath) {
		WebDavServletComponent servletComp = servletFactory.create(rootPath, contextPath);
		return servletComp.servlet();
	}

}
