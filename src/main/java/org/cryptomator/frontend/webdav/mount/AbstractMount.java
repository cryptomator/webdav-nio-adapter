package org.cryptomator.frontend.webdav.mount;

import org.cryptomator.frontend.webdav.WebDavServerHandle;
import org.cryptomator.frontend.webdav.servlet.WebDavServletController;
import org.cryptomator.integrations.mount.Mount;
import org.cryptomator.integrations.mount.UnmountFailedException;

public abstract class AbstractMount implements Mount {

	protected final WebDavServerHandle serverHandle;
	protected final WebDavServletController servlet;

	public AbstractMount(WebDavServerHandle serverHandle, WebDavServletController servlet) {
		this.serverHandle = serverHandle;
		this.servlet = servlet;
	}

	@Override
	public void unmout() throws UnmountFailedException {
		servlet.stop();
	}

	@Override
	public void close() throws UnmountFailedException {
		Mount.super.close();
		serverHandle.close();
	}
}
