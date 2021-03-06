package org.cryptomator.frontend.webdav.mount;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import org.cryptomator.frontend.webdav.VersionCompare;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;

class MacShellScriptMounter implements MounterStrategy {

	private static final Logger LOG = LoggerFactory.getLogger(MacShellScriptMounter.class);
	private static final Path VOLUMES_PATH = Paths.get("/Volumes");
	private static final boolean IS_OS_MAC = System.getProperty("os.name").contains("Mac OS X");
	private static final String OS_VERSION = System.getProperty("os.version");

	@Override
	public boolean isApplicable() {
		try {
			// Fail fast for systems >= 10.9
			if (!IS_OS_MAC || VersionCompare.compareVersions(OS_VERSION, "10.10") < 0) {
				return false;
			}
		} catch (NumberFormatException e) {
			return false;
		}

		try {
			Set<PosixFilePermission> perms = Files.getPosixFilePermissions(VOLUMES_PATH);
			if (perms.contains(PosixFilePermission.GROUP_WRITE)) {
				return true;
			} else {
				LOG.warn("No write permissions on {}. This should be fixed!", VOLUMES_PATH);
				return false;
			}
		} catch (IOException e) {
			LOG.warn("Could not determine permissions for {}", VOLUMES_PATH);
			return false;
		}
	}

	@Override
	public Mount mount(URI uri, MountParams mountParams) throws CommandFailedException {
		Path mountPath = VOLUMES_PATH.resolve("Cryptomator_" + Long.toHexString(crc32(uri.toASCIIString())));
		try {
			String mountName = Iterables.getLast(Splitter.on("/").omitEmptyStrings().split(uri.getPath()));
			Files.createDirectory(mountPath);

			ProcessBuilder mountCmd = new ProcessBuilder("sh", "-c", "mount_webdav -S -v " + mountName + " \"" + uri.toASCIIString() + "\" \"" + mountPath + "\"");
			ProcessUtil.assertExitValue(ProcessUtil.startAndWaitFor(mountCmd, 30, TimeUnit.SECONDS), 0);
			LOG.debug("Mounted {} to {}.", uri.toASCIIString(), mountPath);
			return new MountImpl(uri, mountPath);
		} catch (IOException | CommandFailedException e) {
			try {
				// cleanup:
				Files.deleteIfExists(mountPath);
			} catch (IOException e1) {
				e.addSuppressed(e1);
			}
			throw new CommandFailedException(e);
		}
	}

	private long crc32(String str) {
		CRC32 crc32 = new CRC32();
		crc32.update(str.getBytes(StandardCharsets.UTF_8));
		return crc32.getValue();
	}

	private static class MountImpl implements Mount {

		private final Path mountPath;
		private final ProcessBuilder revealCommand;
		private final ProcessBuilder unmountCommand;
		private final URI uri;

		private MountImpl(URI uri, Path mountPath) {
			this.mountPath = mountPath;
			this.uri = uri;
			this.revealCommand = new ProcessBuilder("open", mountPath.toString());
			this.unmountCommand = new ProcessBuilder("sh", "-c", "diskutil umount \"" + mountPath + "\"");
		}

		@Override
		public void unmount() throws CommandFailedException {
			if (!Files.isDirectory(mountPath)) {
				// unmounting a mounted drive will delete the associated mountpoint (at least under OS X 10.11)
				LOG.debug("Volume already unmounted.");
				return;
			}
			ProcessUtil.assertExitValue(ProcessUtil.startAndWaitFor(unmountCommand, 10, TimeUnit.SECONDS), 0);
			try {
				Files.deleteIfExists(mountPath);
			} catch (IOException e) {
				LOG.warn("Could not delete {} due to exception: {}", mountPath, e.getMessage());
			}
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
