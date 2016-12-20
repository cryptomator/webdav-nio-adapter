package org.cryptomator.frontend.webdav.servlet;

import java.net.URI;
import java.util.Map;

import javax.inject.Inject;

import org.cryptomator.frontend.webdav.ServerLifecycleException;
import org.cryptomator.frontend.webdav.mount.Mounter;
import org.cryptomator.frontend.webdav.mount.Mounter.CommandFailedException;
import org.cryptomator.frontend.webdav.mount.Mounter.Mount;
import org.cryptomator.frontend.webdav.mount.Mounter.MountParam;
import org.cryptomator.frontend.webdav.servlet.WebDavServletModule.ContextRoot;
import org.cryptomator.frontend.webdav.servlet.WebDavServletModule.PerServlet;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PerServlet
public class WebDavServletController {

	private static final Logger LOG = LoggerFactory.getLogger(WebDavServletController.class);

	private final ServletContextHandler contextHandler;
	private final ContextHandlerCollection contextHandlerCollection;
	private final URI uri;
	private final Mounter mounter;

	@Inject
	WebDavServletController(ServletContextHandler contextHandler, ContextHandlerCollection contextHandlerCollection, @ContextRoot URI uri, Mounter mounter) {
		this.contextHandler = contextHandler;
		this.contextHandlerCollection = contextHandlerCollection;
		this.uri = uri;
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
			LOG.info("WebDavServlet started: " + uri);
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
			LOG.info("WebDavServlet stopped: " + uri);
		} catch (Exception e) {
			throw new ServerLifecycleException("Servlet couldn't be stopped", e);
		}
	}

	/**
	 * Tries to mount the resource served by this servlet as a WebDAV drive on the local machine.
	 * 
	 * @param mountParams Optional mount parameters, that may be required for certain operating systems.
	 * @return A {@link Mount} instance allowing unmounting and revealing the drive.
	 * @throws CommandFailedException If mounting failed.
	 */
	public Mount mount(Map<MountParam, String> mountParams) throws CommandFailedException {
		if (!contextHandler.isStarted()) {
			throw new IllegalStateException("Mounting only possible for running servlets.");
		}
		return mounter.mount(uri, mountParams);
	}

}
