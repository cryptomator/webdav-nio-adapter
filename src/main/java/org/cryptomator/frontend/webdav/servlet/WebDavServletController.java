package org.cryptomator.frontend.webdav.servlet;

import org.cryptomator.frontend.webdav.ContextPathRegistry;
import org.cryptomator.frontend.webdav.ServerLifecycleException;
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
	private final ContextPathRegistry contextPathRegistry;
	private final String contextPath;

	WebDavServletController(ServletContextHandler contextHandler, ContextHandlerCollection contextHandlerCollection, ServerConnector connector, ContextPathRegistry contextPathRegistry, String contextPath) {
		this.contextHandler = contextHandler;
		this.contextHandlerCollection = contextHandlerCollection;
		this.connector = connector;
		this.contextPathRegistry = contextPathRegistry;
		this.contextPath = contextPath;
	}

	/**
	 * Convenience function to start this servlet.
	 * 
	 * @throws ServerLifecycleException If the servlet could not be started for any unexpected reason.
	 */
	public void start() throws ServerLifecycleException {
		try {
			contextPathRegistry.add(contextPath);
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
			contextPathRegistry.remove(contextPath);
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
