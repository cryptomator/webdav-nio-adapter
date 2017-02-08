package org.cryptomator.frontend.webdav.mount;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
class MacAppleScriptMounter implements MounterStrategy {

	private static final Logger LOG = LoggerFactory.getLogger(MacAppleScriptMounter.class);

	@Inject
	MacAppleScriptMounter() {
	}

	@Override
	public boolean isApplicable() {
		return SystemUtils.IS_OS_MAC_OSX && // since macOS 10.10+
				!SystemUtils.IS_OS_MAC_OSX_MAVERICKS && // 10.9
				!SystemUtils.IS_OS_MAC_OSX_MOUNTAIN_LION; // 10.8; older version not supported by Java 8
	}

	@Override
	public Mount mount(URI uri, Map<MountParam, String> mountParams) throws CommandFailedException {
		try {
			String mountAppleScript = String.format("mount volume \"%s\"", uri.toASCIIString());
			ProcessBuilder mount = new ProcessBuilder("/usr/bin/osascript", "-e", mountAppleScript);
			Process mountProcess = mount.start();
			String stdout = ProcessUtil.toString(mountProcess.getInputStream(), StandardCharsets.UTF_8);
			ProcessUtil.waitFor(mountProcess, 1, TimeUnit.SECONDS);
			ProcessUtil.assertExitValue(mountProcess, 0);
			String volumeIdentifier = StringUtils.trim(StringUtils.removeStart(stdout, "file "));
			String waitAppleScript1 = String.format("tell application \"Finder\" to repeat while not (\"%s\" exists)", volumeIdentifier);
			String waitAppleScript2 = "delay 0.1";
			String waitAppleScript3 = "end repeat";
			ProcessBuilder wait = new ProcessBuilder("/usr/bin/osascript", "-e", waitAppleScript1, "-e", waitAppleScript2, "-e", waitAppleScript3);
			Process waitProcess = wait.start();
			ProcessUtil.waitFor(waitProcess, 5, TimeUnit.SECONDS);
			ProcessUtil.assertExitValue(waitProcess, 0);
			LOG.debug("Mounted {}.", uri.toASCIIString());
			return new MountImpl(volumeIdentifier);
		} catch (IOException e) {
			throw new CommandFailedException(e);
		}
	}

	private static class MountImpl implements Mount {

		private final ProcessBuilder revealCommand;
		private final ProcessBuilder unmountCommand;

		private MountImpl(String volumeIdentifier) {
			String openAppleScript = String.format("tell application \"Finder\" to open \"%s\"", volumeIdentifier);
			String activateAppleScript = String.format("tell application \"Finder\" to activate \"%s\"", volumeIdentifier);
			String ejectAppleScript = String.format("tell application \"Finder\" to if \"%s\" exists then eject \"%s\"", volumeIdentifier, volumeIdentifier);

			this.revealCommand = new ProcessBuilder("/usr/bin/osascript", "-e", openAppleScript, "-e", activateAppleScript);
			this.unmountCommand = new ProcessBuilder("/usr/bin/osascript", "-e", ejectAppleScript);
		}

		@Override
		public void unmount() throws CommandFailedException {
			try {
				Process proc = unmountCommand.start();
				ProcessUtil.waitFor(proc, 1, TimeUnit.SECONDS);
				ProcessUtil.assertExitValue(proc, 0);
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
