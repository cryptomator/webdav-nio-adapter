package org.cryptomator.frontend.webdav.mount;

import org.cryptomator.frontend.webdav.ServerLifecycleException;
import org.cryptomator.frontend.webdav.WebDavServerHandle;
import org.cryptomator.frontend.webdav.WebDavServerManager;
import org.cryptomator.frontend.webdav.servlet.WebDavServletController;
import org.cryptomator.integrations.mount.Mount;
import org.cryptomator.integrations.mount.MountBuilder;
import org.cryptomator.integrations.mount.MountFailedException;
import org.jetbrains.annotations.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.file.Path;

public abstract class AbstractMountBuilder implements MountBuilder {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractMountBuilder.class);

	protected final Path vfsRoot;
	protected int port;

	public AbstractMountBuilder(Path vfsRoot) {
		this.vfsRoot = vfsRoot;
	}

	@Override
	public MountBuilder setPort(@Range(from = 0L, to = 32767L) int port) {
		this.port = port;
		return this;
	}

	@Override
	public final Mount mount() throws MountFailedException {
		WebDavServerHandle serverHandle;
		try {
			serverHandle = WebDavServerManager.getOrCreateServer(port);
		} catch (ServerLifecycleException e) {
			throw new MountFailedException("Failed to start server", e);
		}

		try {
			WebDavServletController servlet;
			try {
				servlet = serverHandle.server().createWebDavServlet(vfsRoot, "TODO"); // TODO api needs a volume name
				servlet.start();
			} catch (ServerLifecycleException e) {
				throw new MountFailedException("Failed to create WebDAV servlet", e);
			}

			var uri = servlet.getServletRootUri();
			LOG.info("Mounting {}...", uri);

			return this.mount(serverHandle, servlet, uri);
		} catch (MountFailedException e) {
			serverHandle.close();
			throw e;
		}
	}

	protected abstract Mount mount(WebDavServerHandle serverHandle, WebDavServletController servlet, URI uri) throws MountFailedException;
}
