package org.cryptomator.frontend.webdav.servlet;

import org.cryptomator.frontend.webdav.ServerLifecycleException;
import org.cryptomator.frontend.webdav.mount.LegacyMounter;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;

public class WebDavServletController {

	private static final Logger LOG = LoggerFactory.getLogger(WebDavServletController.class);

	private final ServletContextHandler contextHandler;
	private final ContextHandlerCollection contextHandlerCollection;
	private final ServerConnector connector;
	private final String contextPath;
	private final LegacyMounter mounter;

	WebDavServletController(ServletContextHandler contextHandler, ContextHandlerCollection contextHandlerCollection, ServerConnector connector, String contextPath, LegacyMounter mounter) {
		this.contextHandler = contextHandler;
		this.contextHandlerCollection = contextHandlerCollection;
		this.connector = connector;
		this.contextPath = contextPath;
		this.mounter = mounter;
	}

	/**
	 * Convenience function to start this servlet.
	 * 
	 * @throws ServerLifecycleException If the servlet could not be started for any unexpected reason.
	 */
	public void start() throws ServerLifecycleException {
		try {
			contextHandlerCollection.addHandler(contextHandler);
			contextHandlerCollection.mapContexts();
			contextHandler.start();
			LOG.info("WebDavServlet started: " + contextPath);
		} catch (Exception e) {
			throw new ServerLifecycleException("Servlet couldn't be started", e);
		}
	}

	/**
	 * Convenience function to stop this servlet.
	 * 
	 * @throws ServerLifecycleException If the servlet could not be stopped for any unexpected reason.
	 */
	public void stop() throws ServerLifecycleException {
		try {
			contextHandler.stop();
			contextHandlerCollection.removeHandler(contextHandler);
			contextHandlerCollection.mapContexts();
			LOG.info("WebDavServlet stopped: " + contextPath);
		} catch (Exception e) {
			throw new ServerLifecycleException("Servlet couldn't be stopped", e);
		}
	}

	/**
	 * @return A new http URI constructed from the servers bind addr and port as well as this servlet's contextPath.
	 */
	public URI getServletRootUri() {
		return getServletRootUri(connector.getHost());
	}

	private URI getServletRootUri(String hostname) {
		try {
			return new URI("http", null, hostname, connector.getLocalPort(), contextPath, null, null);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Unable to construct valid URI for given contextPath.", e);
		}
	}

}
