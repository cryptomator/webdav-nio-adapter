package org.cryptomator.frontend.webdav.mount;

import java.net.URI;
import java.net.URL;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FallbackMounter implements MounterStrategy {

	private static final Logger LOG = LoggerFactory.getLogger(FallbackMounter.class);

	//TODO: maybe return Fallback mount?
	@Override
	public Mount mount(URI uri, MountParams mountParams) throws CommandFailedException {
		LOG.warn("No applicable strategy has been found for your system. Please use a WebDAV client of your choice to mount: {}", uri);
		throw new CommandFailedException("No mounting strategy found.");
	}

	@Override
	public boolean isApplicable() {
		return true;
	}

	static class FallbackMount implements Mounter.Mount {

		private final URI uri;

		FallbackMount(URI uri){
			this.uri = uri;
		}

		@Override
		public URI getURIofWebDAVDirectory() {
			return uri;
		}

		@Override
		public Optional<UnmountOperation> forced() {
			return Optional.empty();
		}

		@Override
		public void reveal() throws CommandFailedException {
			throw new CommandFailedException("Not mounted into filesystem.");
		}

		@Override
		public void reveal(Revealer revealer) throws RevealException {
			throw new RevealException("Not mounted into filesystem.");
		}

		@Override
		public void unmount() throws CommandFailedException {

		}
	}

}
