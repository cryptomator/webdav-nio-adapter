package org.cryptomator.frontend.webdav.mount;

import org.cryptomator.frontend.webdav.WebDavServerHandle;
import org.cryptomator.frontend.webdav.servlet.WebDavServletController;
import org.cryptomator.integrations.common.Priority;
import org.cryptomator.integrations.mount.*;

import java.net.URI;
import java.nio.file.Path;
import java.util.Set;

@Priority(Priority.FALLBACK)
public class FallbackMounter implements MountService {

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
	public int getDefaultLoopbackPort(){
		return 0;
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
			return new MountImpl(serverHandle, servlet, uri);
		}

	}

	private static class MountImpl extends AbstractMount {
		private final URI uri;

		public MountImpl(WebDavServerHandle serverHandle, WebDavServletController servlet, URI uri) {
			super(serverHandle, servlet);
			this.uri = uri;
		}

		@Override
		public Mountpoint getMountpoint() {
			return Mountpoint.forUri(uri);
		}

	}
}
