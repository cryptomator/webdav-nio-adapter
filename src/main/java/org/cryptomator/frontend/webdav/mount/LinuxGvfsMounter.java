package org.cryptomator.frontend.webdav.mount;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class LinuxGvfsMounter extends VfsMountingStrategy implements MounterStrategy {

	private static final Logger LOG = LoggerFactory.getLogger(LinuxGvfsMounter.class);
	private static final String DEFAULT_GVFS_SCHEME = "dav";
	private static final boolean IS_OS_LINUX = System.getProperty("os.name").toLowerCase().contains("linux");

	@Override
	public boolean isApplicable() {
		if (!IS_OS_LINUX) {
			// fail fast (non-blocking)
			return false;
		}

		// check if gvfs is installed:
		assert IS_OS_LINUX;
		try {
			ProcessBuilder checkDependenciesCmd = new ProcessBuilder("which", "gvfs-mount", "xdg-open");
			ProcessUtil.assertExitValue(ProcessUtil.startAndWaitFor(checkDependenciesCmd, 500, TimeUnit.MILLISECONDS), 0);
			return true;
		} catch (CommandFailedException e) {
			return false;
		}
	}

	@Override
	public Mount mount(URI uri, MountParams mountParams) throws CommandFailedException {
		try {
			URI schemeCorrectedUri = new URI(mountParams.getOrDefault(MountParam.PREFERRED_GVFS_SCHEME, DEFAULT_GVFS_SCHEME), uri.getSchemeSpecificPart(), null);
			ProcessBuilder mountCmd = new ProcessBuilder("sh", "-c", "gvfs-mount \"" + schemeCorrectedUri.toASCIIString() + "\"");
			ProcessUtil.assertExitValue(ProcessUtil.startAndWaitFor(mountCmd, 5, TimeUnit.SECONDS), 0);
			LOG.debug("Mounted {}", schemeCorrectedUri.toASCIIString());
			return new MountGvfsImpl(schemeCorrectedUri);
		} catch (URISyntaxException e) {
			throw new IllegalStateException("URI constructed from elements known to be valid.", e);
		}
	}

	private class MountGvfsImpl extends MountImpl implements Mount {

		private MountGvfsImpl(URI uri) {
			this.revealCmd = new ProcessBuilder("sh", "-c", "gvfs-open \"" + uri.toASCIIString() + "\"");
			this.isMountedCmd = new ProcessBuilder("sh", "-c", "test `gvfs-mount --list | grep \"" + uri.toASCIIString() + "\" | wc -l` -eq 1");
			this.unmountCmd = new ProcessBuilder("sh", "-c", "gvfs-mount -u \"" + uri.toASCIIString() + "\"");
		}

	}

}
