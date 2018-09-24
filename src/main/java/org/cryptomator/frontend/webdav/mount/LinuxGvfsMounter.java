package org.cryptomator.frontend.webdav.mount;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class LinuxGvfsMounter implements MounterStrategy {

	private static final Logger LOG = LoggerFactory.getLogger(LinuxGvfsMounter.class);
	private static final String DEFAULT_GVFS_SCHEME = "dav";
	private static final boolean IS_OS_LINUX = System.getProperty("os.name").toLowerCase().contains("linux");
	private static String mountCommand = null;

	@Override
	public boolean isApplicable() {
		if (!IS_OS_LINUX) {
			// fail fast (non-blocking)
			return false;
		}

		// check if gio or gvfs-mount is installed:
		assert IS_OS_LINUX;
		try {
			mountCommand = "gio";
			ProcessBuilder checkDependenciesCmd = new ProcessBuilder("which", mountCommand, "xdg-open");
			ProcessUtil.assertExitValue(ProcessUtil.startAndWaitFor(checkDependenciesCmd, 500, TimeUnit.MILLISECONDS), 0);
			return true;
		} catch (CommandFailedException e) {
			try {
				mountCommand = "gvfs-mount";
				ProcessBuilder checkDependenciesCmd = new ProcessBuilder("which", mountCommand);
				ProcessUtil.assertExitValue(ProcessUtil.startAndWaitFor(checkDependenciesCmd, 500, TimeUnit.MILLISECONDS), 0);
				return true;
			} catch (CommandFailedException cfe) {
				return false;
			}
		}
	}

	@Override
	public Mount mount(URI uri, MountParams mountParams) throws CommandFailedException {
		try {
			URI schemeCorrectedUri = new URI(mountParams.getOrDefault(MountParam.PREFERRED_GVFS_SCHEME, DEFAULT_GVFS_SCHEME), uri.getSchemeSpecificPart(), null);
			ProcessBuilder mountCmd = (mountCommand.equals("gio")) ?
					new ProcessBuilder("sh", "-c", "gio mount \"" + schemeCorrectedUri.toASCIIString() + "\"") :
					new ProcessBuilder("sh", "-c", "gvfs-mount \"" + schemeCorrectedUri.toASCIIString() + "\"");
			ProcessUtil.assertExitValue(ProcessUtil.startAndWaitFor(mountCmd, 5, TimeUnit.SECONDS), 0);
			LOG.debug("Mounted {}", schemeCorrectedUri.toASCIIString());
			return new MountImpl(schemeCorrectedUri);
		} catch (URISyntaxException e) {
			throw new IllegalStateException("URI constructed from elements known to be valid.", e);
		}
	}

	private static class MountImpl implements Mount {

		private final ProcessBuilder revealCmd;
		private final ProcessBuilder isMountedCmd;
		private final ProcessBuilder unmountCmd;

		private MountImpl(URI uri) {
			if (mountCommand.equals("gio")) {
				this.revealCmd = new ProcessBuilder("sh", "-c", "gio open \"" + uri.toASCIIString() + "\"");
				this.isMountedCmd = new ProcessBuilder("sh", "-c", "test `gio mount --list | grep \"" + uri.toASCIIString() + "\" | wc -l` -eq 1");
				this.unmountCmd = new ProcessBuilder("sh", "-c", "gio mount -u \"" + uri.toASCIIString() + "\"");
			} else {
				this.revealCmd = new ProcessBuilder("sh", "-c", "gvfs-open \"" + uri.toASCIIString() + "\"");
				this.isMountedCmd = new ProcessBuilder("sh", "-c", "test `gvfs-mount --list | grep \"" + uri.toASCIIString() + "\" | wc -l` -eq 1");
				this.unmountCmd = new ProcessBuilder("sh", "-c", "gvfs-mount -u \"" + uri.toASCIIString() + "\"");
			}
		}

		@Override
		public void unmount() throws CommandFailedException {
			Process isMountedProcess = ProcessUtil.startAndWaitFor(isMountedCmd, 5, TimeUnit.SECONDS);
			if (isMountedProcess.exitValue() == 0) {
				// only unmount if volume is still mounted, noop otherwise
				ProcessUtil.assertExitValue(ProcessUtil.startAndWaitFor(unmountCmd, 5, TimeUnit.SECONDS), 0);
			}
		}

		@Override
		public void reveal() throws CommandFailedException {
			ProcessUtil.startAndWaitFor(revealCmd, 5, TimeUnit.SECONDS);
		}

	}

}
