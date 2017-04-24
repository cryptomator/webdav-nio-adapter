package org.cryptomator.frontend.webdav.mount;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FallbackMounter implements MounterStrategy {

	private static final Logger LOG = LoggerFactory.getLogger(FallbackMounter.class);

	@Override
	public Mount mount(URI uri, MountParams mountParams) throws CommandFailedException {
		LOG.warn("No applicable strategy has been found for your system. Please use a WebDAV client of your choice to mount: {}", uri);
		throw new CommandFailedException("No mounting strategy found.");
	}

	@Override
	public boolean isApplicable() {
		return false;
	}

}
