package org.cryptomator.frontend.webdav.mount;

import org.cryptomator.frontend.webdav.VersionCompare;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class MacAppleScriptMounter implements MounterStrategy {

	private static final Logger LOG = LoggerFactory.getLogger(MacAppleScriptMounter.class);
	private static final boolean IS_OS_MAC = System.getProperty("os.name").contains("Mac OS X");
	private static final String OS_VERSION = System.getProperty("os.version");
	private static final Pattern MOUNT_PATTERN = Pattern.compile(".* on (\\S+) \\(.*\\)"); // catches mount point in "http://host:port/foo/ on /Volumes/foo (webdav, nodev, noexec, nosuid)"

	@Override
	public boolean isApplicable() {
		try {
			return IS_OS_MAC && VersionCompare.compareVersions(OS_VERSION, "10.10") >= 0; // since macOS 10.10+
		} catch (NumberFormatException e) {
			return false;
		}
	}

	@Override
	public Mount mount(URI uri, MountParams mountParams) throws CommandFailedException {
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
				return new MountImpl(uri, Paths.get(mountPoint));
			} else {
				throw new CommandFailedException("Mount succeeded, but failed to determine mount point in string: " + stdout);
			}
		} catch (IOException e) {
			throw new CommandFailedException(e);
		}
	}

	private static class MountImpl implements Mount {

		private final Path mountPath;
		private final ProcessBuilder revealCommand;
		private final ProcessBuilder unmountCommand;
		private final URI uri;
		private final ProcessBuilder forcedUnmountCommand;

		private MountImpl(URI uri, Path mountPath) {
			this.mountPath = mountPath;
			this.uri = uri;
			this.revealCommand = new ProcessBuilder("open", mountPath.toString());
			this.unmountCommand = new ProcessBuilder("sh", "-c", "diskutil umount \"" + mountPath + "\"");
			this.forcedUnmountCommand = new ProcessBuilder("sh", "-c", "diskutil umount force \"" + mountPath + "\"");
		}

		@Override
		public void unmount() throws CommandFailedException {
			unmount(unmountCommand);
		}

		@Override
		public Optional<UnmountOperation> forced() {
			return Optional.of(() -> unmount(forcedUnmountCommand));
		}

		private void unmount(ProcessBuilder command) throws CommandFailedException {
			if (!Files.isDirectory(mountPath)) {
				// unmounting a mounted drive will delete the associated mountpoint (at least under OS X 10.11)
				LOG.debug("Volume already unmounted.");
				return;
			}
			ProcessUtil.assertExitValue(ProcessUtil.startAndWaitFor(command, 10, TimeUnit.SECONDS), 0);
		}

		@Override
		public Optional<Path> getMountPoint() {
			return Optional.of(mountPath);
		}

		@Override
		public URI getWebDavUri() {
			return uri;
		}

		@Override
		public void reveal() throws CommandFailedException {
			ProcessUtil.assertExitValue(ProcessUtil.startAndWaitFor(revealCommand, 10, TimeUnit.SECONDS), 0);
		}

		@Override
		public void reveal(Revealer revealer) throws Exception {
			revealer.reveal(mountPath);
		}

	}

}
