package org.cryptomator.frontend.webdav.mount;

import java.net.URI;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
class FallbackMounter implements MounterStrategy {

	private static final Logger LOG = LoggerFactory.getLogger(FallbackMounter.class);

	@Inject
	FallbackMounter() {
	}

	@Override
	public Mount mount(URI uri, Map<MountParam, String> mountParams) throws CommandFailedException {
		LOG.warn("No applicable strategy has been found for your system. Please use a WebDAV client of your choice to mount: {}", uri);
		throw new CommandFailedException("No mounting strategy found.");
	}

	@Override
	public boolean isApplicable() {
		return false;
	}

}
