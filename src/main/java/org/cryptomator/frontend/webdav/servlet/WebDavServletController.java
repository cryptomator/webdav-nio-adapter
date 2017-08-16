package org.cryptomator.frontend.webdav.servlet;

import java.net.URI;
import java.net.URISyntaxException;

import javax.inject.Inject;

import org.cryptomator.frontend.webdav.ServerLifecycleException;
import org.cryptomator.frontend.webdav.mount.MountParam;
import org.cryptomator.frontend.webdav.mount.MountParams;
import org.cryptomator.frontend.webdav.mount.Mounter;
import org.cryptomator.frontend.webdav.mount.Mounter.CommandFailedException;
import org.cryptomator.frontend.webdav.mount.Mounter.Mount;
import org.cryptomator.frontend.webdav.servlet.WebDavServletModule.ContextPath;
import org.cryptomator.frontend.webdav.servlet.WebDavServletModule.PerServlet;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PerServlet
public class WebDavServletController {

	private static final Logger LOG = LoggerFactory.getLogger(WebDavServletController.class);

	private final ServletContextHandler contextHandler;
	private final ContextHandlerCollection contextHandlerCollection;
	private final ServerConnector connector;
	private final String contextPath;
	private final Mounter mounter;

	@Inject
	WebDavServletController(ServletContextHandler contextHandler, ContextHandlerCollection contextHandlerCollection, ServerConnector connector, @ContextPath String contextPath, Mounter mounter) {
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

	/**
	 * Tries to mount the resource served by this servlet as a WebDAV drive on the local machine.
	 * 
	 * @param mountParams Optional mount parameters, that may be required for certain operating systems.
	 * @return A {@link Mount} instance allowing unmounting and revealing the drive.
	 * @throws CommandFailedException If mounting failed.
	 */
	public Mount mount(MountParams mountParams) throws CommandFailedException {
		if (!contextHandler.isStarted()) {
			throw new IllegalStateException("Mounting only possible for running servlets.");
		}
		URI uri = getServletRootUri(mountParams.getOrDefault(MountParam.WEBDAV_HOSTNAME, connector.getHost()));
		LOG.info("Mounting {} using {}", uri, mounter.getClass().getName());
		return mounter.mount(uri, mountParams);
	}

}
