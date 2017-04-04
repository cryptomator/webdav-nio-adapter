package org.cryptomator.frontend.webdav.mount;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class LinuxGvfsMounter implements MounterStrategy {

	private static final Logger LOG = LoggerFactory.getLogger(LinuxGvfsMounter.class);
	private static final String DEFAULT_GVFS_SCHEME = "dav";

	@Override
	public boolean isApplicable() {
		if (!SystemUtils.IS_OS_LINUX) {
			// fail fast (non-blocking)
			return false;
		}

		// check if gvfs is installed:
		assert SystemUtils.IS_OS_LINUX;
		try {
			ProcessBuilder checkDependenciesCmd = new ProcessBuilder("which", "gvfs-mount", "xdg-open");
			Process checkDependenciesProcess = checkDependenciesCmd.start();
			ProcessUtil.waitFor(checkDependenciesProcess, 500, TimeUnit.MILLISECONDS);
			ProcessUtil.assertExitValue(checkDependenciesProcess, 0);
			return true;
		} catch (CommandFailedException | IOException e) {
			return false;
		}
	}

	@Override
	public Mount mount(URI uri, Map<MountParam, String> mountParams) throws CommandFailedException {
		try {
			URI schemeCorrectedUri = new URI(mountParams.getOrDefault(MountParam.PREFERRED_GVFS_SCHEME, DEFAULT_GVFS_SCHEME), uri.getSchemeSpecificPart(), null);
			ProcessBuilder mountCmd = new ProcessBuilder("sh", "-c", "gvfs-mount \"" + schemeCorrectedUri.toASCIIString() + "\"");
			Process mountProcess = mountCmd.start();
			ProcessUtil.waitFor(mountProcess, 5, TimeUnit.SECONDS);
			ProcessUtil.assertExitValue(mountProcess, 0);
			LOG.debug("Mounted {}", schemeCorrectedUri.toASCIIString());
			return new MountImpl(schemeCorrectedUri);
		} catch (IOException e) {
			throw new CommandFailedException(e);
		} catch (URISyntaxException e) {
			throw new IllegalStateException("URI constructed from elements known to be valid.", e);
		}
	}

	private static class MountImpl implements Mount {

		private final ProcessBuilder revealCmd;
		private ProcessBuilder isMountedCmd;
		private final ProcessBuilder unmountCmd;

		private MountImpl(URI uri) {
			this.revealCmd = new ProcessBuilder("sh", "-c", "gvfs-open \"" + uri.toASCIIString() + "\"");
			this.isMountedCmd = new ProcessBuilder("sh", "-c", "test `gvfs-mount --list | grep \"" + uri.toASCIIString() + "\" | wc -l` -eq 1");
			this.unmountCmd = new ProcessBuilder("sh", "-c", "gvfs-mount -u \"" + uri.toASCIIString() + "\"");
		}

		@Override
		public void unmount() throws CommandFailedException {
			try {
				Process isMountedProcess = isMountedCmd.start();
				ProcessUtil.waitFor(isMountedProcess, 1, TimeUnit.SECONDS);

				if (isMountedProcess.exitValue() == 0) {
					// only unmount if volume is still mounted, noop otherwise
					Process proc = unmountCmd.start();
					ProcessUtil.waitFor(proc, 1, TimeUnit.SECONDS);
					ProcessUtil.assertExitValue(proc, 0);
				}
			} catch (IOException e) {
				throw new CommandFailedException(e);
			}
		}

		@Override
		public void reveal() throws CommandFailedException {
			try {
				Process proc = revealCmd.start();
				ProcessUtil.waitFor(proc, 2, TimeUnit.SECONDS);
				ProcessUtil.assertExitValue(proc, 0);
			} catch (IOException e) {
				throw new CommandFailedException(e);
			}
		}

	}

}
