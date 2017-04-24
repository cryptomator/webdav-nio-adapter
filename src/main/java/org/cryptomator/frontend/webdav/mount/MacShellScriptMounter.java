package org.cryptomator.frontend.webdav.mount;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class MacShellScriptMounter implements MounterStrategy {

	private static final Logger LOG = LoggerFactory.getLogger(MacShellScriptMounter.class);
	private static final Path VOLUMES_PATH = Paths.get("/Volumes");

	@Override
	public boolean isApplicable() {
		if (!SystemUtils.IS_OS_MAC_OSX_MAVERICKS && !SystemUtils.IS_OS_MAC_OSX_MOUNTAIN_LION) {
			// Fail fast for systems other than 10.8 and 10.9.
			// >=10.10 handled by MacAppleScriptMounter
			// <10.8 is not supported by Java 8 anyway.
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
			String mountName = StringUtils.substringAfterLast(StringUtils.removeEnd(uri.getPath(), "/"), "/");
			Files.createDirectory(mountPath);

			ProcessBuilder mountCmd = new ProcessBuilder("sh", "-c", "mount_webdav -S -v " + mountName + " " + uri.toASCIIString() + " " + mountPath);
			Process mountProcess = mountCmd.start();
			ProcessUtil.waitFor(mountProcess, 1, TimeUnit.SECONDS);
			ProcessUtil.assertExitValue(mountProcess, 0);
			LOG.debug("Mounted {} to {}.", uri.toASCIIString(), mountPath);
			return new MountImpl(mountPath);
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

		private MountImpl(Path mountPath) {
			this.mountPath = mountPath;
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
			try {
				Process proc = unmountCommand.start();
				ProcessUtil.waitFor(proc, 1, TimeUnit.SECONDS);
				ProcessUtil.assertExitValue(proc, 0);
				Files.deleteIfExists(mountPath);
			} catch (IOException e) {
				throw new CommandFailedException(e);
			}
		}

		@Override
		public void reveal() throws CommandFailedException {
			try {
				Process proc = revealCommand.start();
				ProcessUtil.waitFor(proc, 2, TimeUnit.SECONDS);
				ProcessUtil.assertExitValue(proc, 0);
			} catch (IOException e) {
				throw new CommandFailedException(e);
			}
		}

	}

}
