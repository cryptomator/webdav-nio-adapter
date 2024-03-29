package org.cryptomator.frontend.webdav.mount;

import org.cryptomator.frontend.webdav.WebDavServerHandle;
import org.cryptomator.frontend.webdav.servlet.WebDavServletController;
import org.cryptomator.integrations.mount.Mount;
import org.cryptomator.integrations.mount.UnmountFailedException;

import java.io.IOException;

public abstract class AbstractMount implements Mount {

	protected final WebDavServerHandle serverHandle;
	protected final WebDavServletController servlet;

	public AbstractMount(WebDavServerHandle serverHandle, WebDavServletController servlet) {
		this.serverHandle = serverHandle;
		this.servlet = servlet;
	}

	@Override
	public void unmount() throws UnmountFailedException {
		servlet.stop();
	}

	@Override
	public void close() throws UnmountFailedException, IOException {
		Mount.super.close();
		serverHandle.close();
	}
}
