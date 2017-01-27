/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE.txt.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.frontend.webdav;

import java.nio.file.Path;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.cryptomator.frontend.webdav.servlet.WebDavServletComponent;
import org.cryptomator.frontend.webdav.servlet.WebDavServletController;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The WebDAV server, that WebDAV servlets can be added to using {@link #createWebDavServlet()}.
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
	WebDavServer(Server server, ServerConnector connector, WebDavServletFactory servletContextFactory) {
		this.server = server;
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
		try {
			localConnector.stop();
			localConnector.setHost(bindAddr);
			localConnector.setPort(port);
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
	 * Creates a new WebDAV servlet (without starting it yet).
	 * 
	 * @param rootPath The path to the directory which should be served as root resource.
	 * @param contextPath The servlet context path, i.e. the path of the root resource.
	 * @return The controller object for this new servlet
	 */
	public WebDavServletController createWebDavServlet(Path rootPath, String contextPath) throws ServerLifecycleException {
		WebDavServletComponent servletComp = servletFactory.create(rootPath, contextPath);
		return servletComp.servlet();
	}

}
