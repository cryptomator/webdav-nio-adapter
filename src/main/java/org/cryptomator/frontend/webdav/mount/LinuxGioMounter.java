package org.cryptomator.frontend.webdav.mount;

import org.cryptomator.frontend.webdav.WebDavServerHandle;
import org.cryptomator.frontend.webdav.servlet.WebDavServletController;
import org.cryptomator.integrations.common.OperatingSystem;
import org.cryptomator.integrations.common.Priority;
import org.cryptomator.integrations.mount.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Priority(50)
@OperatingSystem(OperatingSystem.Value.LINUX)
public class LinuxGioMounter implements MountService {

	private static final Logger LOG = LoggerFactory.getLogger(LinuxGioMounter.class);
	private static final String WEBDAV_URI_SCHEME = "dav";
	private static final Path USER_HOME = Paths.get(System.getProperty("user.home"));
	private final Path gvfsMountDir;

	public LinuxGioMounter() {
		int uid;
		try {
			uid = (Integer)Files.getAttribute(USER_HOME, "unix:uid");
		} catch (IOException e) {
			uid = 0;
		}
		this.gvfsMountDir = Path.of("/run/user", String.valueOf(uid), "gvfs");
	}

	@Override
	public String displayName() {
		return "WebDAV (gio)";
	}

	@Override
	public boolean isSupported() {
		if( System.getenv().getOrDefault("XDG_CURRENT_DESKTOP", "").equals("KDE")) {
			return false;	//see https://github.com/cryptomator/cryptomator/issues/1381
		}

		if (!Files.isDirectory(gvfsMountDir)) {
			return false;
		}

		// check if gio is installed:
		try {
			ProcessBuilder checkDependenciesCmd = new ProcessBuilder("test", " `command -v gio`");
			ProcessUtil.assertExitValue(ProcessUtil.startAndWaitFor(checkDependenciesCmd, 500, TimeUnit.MILLISECONDS), 0);
			return true;
		} catch (IOException | TimeoutException e) {
			return false;
		}
	}

	@Override
	public Set<MountCapability> capabilities() {
		return Set.of(MountCapability.LOOPBACK_PORT, MountCapability.MOUNT_TO_SYSTEM_CHOSEN_PATH, MountCapability.VOLUME_ID);
	}

	@Override
	public MountBuilder forFileSystem(Path fileSystemRoot) {
		return new MountBuilderImpl(fileSystemRoot);
	}

	private class MountBuilderImpl extends AbstractMountBuilder {

		public MountBuilderImpl(Path vfsRoot) {
			super(vfsRoot);
		}

		@Override
		protected Mount mount(WebDavServerHandle serverHandle, WebDavServletController servlet, URI uri) throws MountFailedException {
			try {
				URI schemeCorrectedUri = new URI(WEBDAV_URI_SCHEME, uri.getSchemeSpecificPart(), null);

				// mount:
				ProcessBuilder mountCmd = new ProcessBuilder("sh", "-c", "gio mount \"" + schemeCorrectedUri.toASCIIString() + "\"");
				Process mountProcess = mountCmd.start();
				ProcessUtil.waitFor(mountProcess, 30, TimeUnit.SECONDS);
				ProcessUtil.assertExitValue(mountProcess, 0);

				// find mount point within gvfsMountDir:
				try (var ds = Files.newDirectoryStream(gvfsMountDir)) {
					var encodedPath = URLEncoder.encode(schemeCorrectedUri.getRawPath(), StandardCharsets.UTF_8);
					for (Path mountPoint : ds) {
						var dirName = mountPoint.getFileName().toString();
						// dirName looks like this: dav:host=localhost,port=42427,ssl=false,prefix=%2Fdix6BcCSNSl5%2Ftest
						if (dirName.contains(schemeCorrectedUri.getHost()) && dirName.contains(encodedPath)) {
							LOG.debug("Mounted {} on {}.", schemeCorrectedUri.toASCIIString(), mountPoint);
							return new MountImpl(serverHandle, servlet, mountPoint, schemeCorrectedUri);
						}
					}
					throw new MountFailedException("Mount succeeded, but failed to determine mount point within dir: " + gvfsMountDir);
				}
			} catch (URISyntaxException e) {
				throw new IllegalStateException("URI constructed from elements known to be valid.", e);
			} catch (IOException | TimeoutException e) {
				throw new MountFailedException("Mounting failed", e);
			}
		}

	}

	private static class MountImpl extends AbstractMount {

		private final Path mountPoint;
		private final ProcessBuilder unmountCommand;

		public MountImpl(WebDavServerHandle serverHandle, WebDavServletController servlet, Path mountPoint, URI uri) {
			super(serverHandle, servlet);
			this.mountPoint = mountPoint;
			this.unmountCommand = new ProcessBuilder("sh", "-c", "gio mount -u \"" + uri.toASCIIString() + "\"");
		}

		@Override
		public Path getMountpoint() {
			return mountPoint;
		}

		@Override
		public void unmount() throws UnmountFailedException {
			if (!Files.isDirectory(mountPoint)) {
				LOG.debug("Volume already unmounted.");
				return;
			}
			try {
				ProcessUtil.assertExitValue(ProcessUtil.startAndWaitFor(unmountCommand, 10, TimeUnit.SECONDS), 0);
				super.unmount();
			} catch (IOException | TimeoutException e) {
				throw new UnmountFailedException(e);
			}
		}
	}

}
