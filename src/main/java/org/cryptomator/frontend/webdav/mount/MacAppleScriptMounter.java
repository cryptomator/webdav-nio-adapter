package org.cryptomator.frontend.webdav.mount;

import org.cryptomator.frontend.webdav.*;
import org.cryptomator.frontend.webdav.servlet.WebDavServletController;
import org.cryptomator.integrations.common.OperatingSystem;
import org.cryptomator.integrations.common.Priority;
import org.cryptomator.integrations.mount.*;
import org.jetbrains.annotations.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.cryptomator.integrations.mount.MountFeature.*;

@Priority(50)
@OperatingSystem(OperatingSystem.Value.MAC)
public class MacAppleScriptMounter implements MountProvider {

	private static final Logger LOG = LoggerFactory.getLogger(MacAppleScriptMounter.class);
	private static final String OS_VERSION = System.getProperty("os.version");
	private static final Pattern MOUNT_PATTERN = Pattern.compile(".* on (\\S+) \\(.*\\)"); // catches mount point in "http://host:port/foo/ on /Volumes/foo (webdav, nodev, noexec, nosuid)"


	@Override
	public String displayName() {
		return "WebDAV (Apple Script)";
	}

	@Override
	public boolean isSupported() {
		try {
			return VersionCompare.compareVersions(OS_VERSION, "10.10") >= 0; // since macOS 10.10+
		} catch (NumberFormatException e) {
			return false;
		}
	}

	@Override
	public Set<MountFeature> supportedFeatures() {
		return Set.of(PORT, UNMOUNT_FORCED);
	}

	@Override
	public @Range(from = 0L, to = 32767L) int getDefaultPort() {
		return 42427;
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
		protected Mount mount(WebDavServerHandle serverHandle, WebDavServletController servlet, URI uri) throws MountFailedException {
			try {
				// mount:
				String mountAppleScript = String.format("mount volume \"%s\"", uri.toASCIIString());
				ProcessBuilder mount = new ProcessBuilder("/usr/bin/osascript", "-e", mountAppleScript);
				Process mountProcess = mount.start();
				ProcessUtil.waitFor(mountProcess, 60, TimeUnit.SECONDS); // huge timeout since the user might need to confirm connecting via http
				ProcessUtil.assertExitValue(mountProcess, 0);

				// verify mounted:
				ProcessBuilder verifyMount = new ProcessBuilder("/bin/sh", "-c", "mount | grep \"" + uri.toASCIIString() + "\"");
				Process verifyProcess = verifyMount.start();
				String stdout = ProcessUtil.toString(verifyProcess.getInputStream(), StandardCharsets.UTF_8);
				ProcessUtil.waitFor(verifyProcess, 10, TimeUnit.SECONDS);
				ProcessUtil.assertExitValue(mountProcess, 0);

				// determine mount point:
				Matcher mountPointMatcher = MOUNT_PATTERN.matcher(stdout);
				if (mountPointMatcher.find()) {
					String mountPoint = mountPointMatcher.group(1);
					LOG.debug("Mounted {} on {}.", uri.toASCIIString(), mountPoint);
					return new MountImpl(serverHandle, servlet, Paths.get(mountPoint));
				} else {
					throw new MountFailedException("Mount succeeded, but failed to determine mount point in string: " + stdout);
				}
			} catch (IOException | LegacyMounter.CommandFailedException e) {
				throw new MountFailedException("Mounting failed");
			}
		}
	}

	private static class MountImpl extends AbstractMount {

		private final Path mountPath;
		private final ProcessBuilder unmountCommand;
		private final ProcessBuilder forcedUnmountCommand;

		private MountImpl(WebDavServerHandle serverHandle, WebDavServletController servlet, Path mountPath) {
			super(serverHandle, servlet);
			this.mountPath = mountPath;
			this.unmountCommand = new ProcessBuilder("sh", "-c", "diskutil umount \"" + mountPath + "\"");
			this.forcedUnmountCommand = new ProcessBuilder("sh", "-c", "diskutil umount force \"" + mountPath + "\"");
		}

		@Override
		public Path getMountpoint() {
			return mountPath;
		}

		@Override
		public void unmount() throws UnmountFailedException {
			unmount(unmountCommand);
		}

		@Override
		public void unmountForced() throws UnmountFailedException {
			unmount(forcedUnmountCommand);
		}

		private void unmount(ProcessBuilder command) throws UnmountFailedException {
			if (!Files.isDirectory(mountPath)) {
				// unmounting a mounted drive will delete the associated mountpoint (at least under OS X 10.11)
				LOG.debug("Volume already unmounted.");
				return;
			}
			try {
				ProcessUtil.assertExitValue(ProcessUtil.startAndWaitFor(command, 10, TimeUnit.SECONDS), 0);
				servlet.stop();
			} catch (LegacyMounter.CommandFailedException e) {
				throw new UnmountFailedException(e);
			}
		}

	}

}
