package org.cryptomator.frontend.webdav.mount;

import org.cryptomator.frontend.webdav.WebDavServerHandle;
import org.cryptomator.frontend.webdav.servlet.WebDavServletController;
import org.cryptomator.integrations.common.Priority;
import org.cryptomator.integrations.mount.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.file.Path;
import java.util.Set;

@Priority(Priority.FALLBACK)
public class FallbackMounter implements MountService {

	private static final Logger LOG = LoggerFactory.getLogger(FallbackMounter.class);

	@Override
	public String displayName() {
		return "WebDAV (Fallback)";
	}

	@Override
	public boolean isSupported() {
		return true;
	}

	@Override
	public Set<MountCapability> capabilities() {
		return Set.of(MountCapability.LOOPBACK_PORT, MountCapability.VOLUME_ID);
	}

	@Override
	public MountBuilder forFileSystem(Path path) {
		return new MountBuilderImpl(path);
	}

	private static class MountBuilderImpl extends AbstractMountBuilder {

		public MountBuilderImpl(Path vfsRoot) {
			super(vfsRoot);
		}

		@Override
		protected Mount mount(WebDavServerHandle serverHandle, WebDavServletController servlet, URI uri) {
			LOG.warn("No applicable strategy has been found for your system. Please use a WebDAV client of your choice to access: {}", uri);
			return new MountImpl(serverHandle, servlet, uri);
		}

	}

	private static class MountImpl extends AbstractMount {
		public MountImpl(WebDavServerHandle serverHandle, WebDavServletController servlet, URI uri) {
			super(serverHandle, servlet);
		}

		@Override
		public Path getMountpoint() {
			// FIXME in API
			throw new UnsupportedOperationException();
		}

	}
}
