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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;

public abstract class AbstractMountBuilder implements MountBuilder {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractMountBuilder.class);

	protected final Path vfsRoot;
	protected String contextPath;
	protected int port;

	public AbstractMountBuilder(Path vfsRoot) {
		this.vfsRoot = vfsRoot;
	}

	@Override
	public MountBuilder setLoopbackPort(@Range(from = 0L, to = 32767L) int port) {
		this.port = port;
		return this;
	}

	@Override
	public MountBuilder setVolumeId(String volumeId) {
		try {
			new URL("http", "localhost", 80, volumeId);
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("Volume id needs to satisfy url path component restrictions", e);
		}
		this.contextPath = volumeId;
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

		boolean success = false;
		try {
			WebDavServletController servlet;
			try {
				servlet = serverHandle.server().createWebDavServlet(vfsRoot, contextPath);
				servlet.start();
			} catch (ServerLifecycleException e) {
				throw new MountFailedException("Failed to create WebDAV servlet", e);
			}

			var uri = servlet.getServletRootUri();
			LOG.info("Mounting {}...", uri);

			var mount = this.mount(serverHandle, servlet, uri);
			success = true;
			return mount;
		} finally {
			if (!success) {
				try {
					serverHandle.close();
				} catch (IOException e) {
					LOG.warn("Terminating server caused I/O error", e);
				}
			}
		}
	}

	protected abstract Mount mount(WebDavServerHandle serverHandle, WebDavServletController servlet, URI uri) throws MountFailedException;
}
