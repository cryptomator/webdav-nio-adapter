package org.cryptomator.frontend.webdav.mount;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import org.cryptomator.frontend.webdav.WebDavServerHandle;
import org.cryptomator.frontend.webdav.servlet.WebDavServletController;
import org.cryptomator.integrations.common.OperatingSystem;
import org.cryptomator.integrations.common.Priority;
import org.cryptomator.integrations.mount.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Priority(50)
@OperatingSystem(OperatingSystem.Value.WINDOWS)
public class WindowsMounter implements MountProvider {

	private static final Logger LOG = LoggerFactory.getLogger(WindowsMounter.class);
	private static final Pattern REG_QUERY_PROXY_OVERRIDES_PATTERN = Pattern.compile("\\s*ProxyOverride\\s+REG_SZ\\s+(.*)\\s*");

	@Override
	public String displayName() {
		return "WebDAV (Windows Explorer)";
	}

	@Override
	public boolean isSupported() {
		return true;
	}

	@Override
	public Set<MountFeature> supportedFeatures() {
		return Set.of(MountFeature.PORT, MountFeature.MOUNT_AS_DRIVE_LETTER, MountFeature.MOUNT_TO_SYSTEM_CHOSEN_PATH, MountFeature.UNMOUNT_FORCED);
	}

	@Override
	public MountBuilder forFileSystem(Path path) {
		return null;
	}

	private static class MountBuilderImpl extends AbstractMountBuilder {

		private Path driveLetter;

		public MountBuilderImpl(Path vfsRoot) {
			super(vfsRoot);
		}

		@Override
		public MountBuilder setMountpoint(Path mountPoint) {
			if (mountPoint.getRoot().equals(mountPoint)) {
				this.driveLetter = mountPoint;
				return this;
			} else {
				throw new IllegalArgumentException("Mount point needs to be a drive letter");
			}
		}


		@Override
		protected Mount mount(WebDavServerHandle serverHandle, WebDavServletController servlet, URI uri) throws MountFailedException {
			try {
				tuneProxyConfigSilently(uri);
				String mountPoint = driveLetter == null //
						? "*" // MOUNT_TO_SYSTEM_CHOSEN_PATH
						: driveLetter.toString(); // MOUNT_AS_DRIVE_LETTER
				String uncPath = "\\\\" + uri.getHost() + "@" + uri.getPort() + "\\DavWWWRoot" + uri.getRawPath().replace('/', '\\');
				ProcessBuilder mount = new ProcessBuilder("net", "use", mountPoint, uncPath, "/persistent:no");
				Process mountProcess = mount.start();
				String stdout = ProcessUtil.toString(mountProcess.getInputStream(), StandardCharsets.UTF_8);
				ProcessUtil.waitFor(mountProcess, 30, TimeUnit.SECONDS);
				ProcessUtil.assertExitValue(mountProcess, 0);
				LOG.debug("Mounted {} on drive {}", uncPath, mountPoint);
				return new MountImpl(serverHandle, servlet, mountPoint);
			} catch (IOException | TimeoutException e) {
				throw new MountFailedException(e);
			}

		}

	}

	private static void tuneProxyConfigSilently(URI uri) {
		try {
			tuneProxyConfig(uri);
		} catch (IOException | TimeoutException e) {
			LOG.warn("Tuning proxy config failed.", e);
		}
	}

	/**
	 * @param uri The URI for which to tune the registry settings
	 * @throws IOException If registry access fails
	 * @throws TimeoutException If registry access does not finish in time
	 * @deprecated TODO overheadhunter: check if this is really necessary.
	 */
	@Deprecated
	private static void tuneProxyConfig(URI uri) throws IOException, TimeoutException {
		// get existing value for ProxyOverride key from reqistry:
		ProcessBuilder regQuery = new ProcessBuilder("reg", "query", "\"HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings\"", "/v", "ProxyOverride");
		Process regQueryProcess = ProcessUtil.startAndWaitFor(regQuery, 5, TimeUnit.SECONDS);
		String regQueryResult = ProcessUtil.toString(regQueryProcess.getInputStream(), StandardCharsets.UTF_8);

		// determine new value for ProxyOverride key:
		Set<String> overrides = new HashSet<>();
		Matcher matcher = REG_QUERY_PROXY_OVERRIDES_PATTERN.matcher(regQueryResult);
		if (regQueryProcess.exitValue() == 0 && matcher.find()) {
			String originalOverrides = matcher.group(1);
			LOG.debug("Original Registry value for ProxyOverride is: {}", originalOverrides);
			Splitter.on(';').split(originalOverrides).forEach(overrides::add);
		}
		overrides.removeIf(s -> s.startsWith(uri.getHost() + ":"));
		overrides.add("<local>");
		overrides.add(uri.getHost());
		overrides.add(uri.getHost() + ":" + uri.getPort());

		// set new value:
		String adjustedOverrides = Joiner.on(';').join(overrides);
		ProcessBuilder regAdd = new ProcessBuilder("reg", "add", "\"HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings\"", "/v", "ProxyOverride", "/d", "\"" + adjustedOverrides + "\"", "/f");
		LOG.debug("Setting Registry value for ProxyOverride to: {}", adjustedOverrides);
		Process regAddProcess = ProcessUtil.startAndWaitFor(regAdd, 5, TimeUnit.SECONDS);
		ProcessUtil.assertExitValue(regAddProcess, 0);
	}

	private static class MountImpl extends AbstractMount {

		private final ProcessBuilder unmountCommand;
		private final ProcessBuilder forcedUnmountCommand;

		private final Path mountpoint;

		public MountImpl(WebDavServerHandle serverHandle, WebDavServletController servlet, String driveLetter) {
			super(serverHandle, servlet);
			this.unmountCommand = new ProcessBuilder("net", "use", driveLetter, "/delete", "/no");
			this.forcedUnmountCommand = new ProcessBuilder("net", "use", driveLetter, "/delete", "/yes");
			this.mountpoint = Paths.get(driveLetter);
		}

		@Override
		public Path getMountpoint() {
			return mountpoint;
		}

		@Override
		public void unmount() throws UnmountFailedException {
			run(unmountCommand);
		}

		@Override
		public void unmountForced() throws UnmountFailedException {
			run(forcedUnmountCommand);
		}

		private void run(ProcessBuilder command) throws UnmountFailedException {
			try {
				ProcessUtil.assertExitValue(ProcessUtil.startAndWaitFor(command, 5, TimeUnit.SECONDS), 0);
			} catch (IOException | TimeoutException e) {
				throw new UnmountFailedException(e);
			}
		}

	}

}
