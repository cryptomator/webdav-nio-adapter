package org.cryptomator.frontend.webdav.mount;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;

class MacAppleScriptMounter implements MounterStrategy {

	private static final Logger LOG = LoggerFactory.getLogger(MacAppleScriptMounter.class);
	private static final boolean IS_OS_MACOSX = System.getProperty("os.name").contains("Mac OS X");
	private static final String[] OS_VERSION = Iterables.toArray(Splitter.on('.').splitToList(System.getProperty("os.version")), String.class);

	@Override
	public boolean isApplicable() {
		try {
			return IS_OS_MACOSX && OS_VERSION.length >= 2 && Integer.parseInt(OS_VERSION[1]) >= 10; // since macOS 10.10+
		} catch (NumberFormatException e) {
			return false;
		}
	}

	@Override
	public Mount mount(URI uri, MountParams mountParams) throws CommandFailedException {
		try {
			ProcessBuilder storeCredentials = new ProcessBuilder("security", "add-internet-password", //
					"-a", "anonymous", //
					"-s", "localhost", //
					"-P", String.valueOf(uri.getPort()), //
					"-r", "http", //
					"-D", "Cryptomator WebDAV Access", //
					"-T", "/System/Library/CoreServices/NetAuthAgent.app/Contents/MacOS/NetAuthSysAgent");
			ProcessUtil.startAndWaitFor(storeCredentials, 10, TimeUnit.SECONDS);
		} catch (CommandFailedException e) {
			LOG.warn("Unable to store credentials for WebDAV access: {}", e.getMessage());
		}
		try {
			String mountAppleScript = String.format("mount volume \"%s\"", uri.toASCIIString());
			ProcessBuilder mount = new ProcessBuilder("/usr/bin/osascript", "-e", mountAppleScript);
			Process mountProcess = mount.start();
			String stdout = ProcessUtil.toString(mountProcess.getInputStream(), StandardCharsets.UTF_8);
			ProcessUtil.waitFor(mountProcess, 30, TimeUnit.SECONDS);
			ProcessUtil.assertExitValue(mountProcess, 0);
			if (!stdout.startsWith("file ")) {
				throw new CommandFailedException("Unexpected mount result: " + stdout);
			}
			assert stdout.startsWith("file ");
			String volumeIdentifier = CharMatcher.whitespace().trimFrom(stdout.substring(5)); // remove preceeding "file "
			String waitAppleScript1 = String.format("tell application \"Finder\" to repeat while not (\"%s\" exists)", volumeIdentifier);
			String waitAppleScript2 = "delay 0.1";
			String waitAppleScript3 = "end repeat";
			ProcessBuilder wait = new ProcessBuilder("/usr/bin/osascript", "-e", waitAppleScript1, "-e", waitAppleScript2, "-e", waitAppleScript3);
			ProcessUtil.assertExitValue(ProcessUtil.startAndWaitFor(wait, 30, TimeUnit.SECONDS), 0);
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
			ProcessUtil.assertExitValue(ProcessUtil.startAndWaitFor(unmountCommand, 10, TimeUnit.SECONDS), 0);
		}

		@Override
		public void reveal() throws CommandFailedException {
			ProcessUtil.assertExitValue(ProcessUtil.startAndWaitFor(revealCommand, 10, TimeUnit.SECONDS), 0);
		}

	}

}
