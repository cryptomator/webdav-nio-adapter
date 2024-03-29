package org.cryptomator.frontend.webdav.mount;

import org.cryptomator.frontend.webdav.WebDavServerHandle;
import org.cryptomator.frontend.webdav.servlet.WebDavServletController;
import org.cryptomator.integrations.common.OperatingSystem;
import org.cryptomator.integrations.common.Priority;
import org.cryptomator.integrations.mount.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Priority(50)
@OperatingSystem(OperatingSystem.Value.WINDOWS)
public class WindowsMounter implements MountService {

	private static final Logger LOG = LoggerFactory.getLogger(WindowsMounter.class);
	private static final Pattern REG_QUERY_PROXY_OVERRIDES_PATTERN = Pattern.compile("\\s*ProxyOverride\\s+REG_SZ\\s+(.*)\\s*");
	private static final String SYSTEM_CHOSEN_MOUNTPOINT = "*";

	@Override
	public String displayName() {
		return "WebDAV (Windows Explorer)";
	}

	@Override
	public boolean isSupported() {
		return true;
	}

	@Override
	public Set<MountCapability> capabilities() {
		return Set.of(MountCapability.LOOPBACK_PORT, MountCapability.LOOPBACK_HOST_NAME, MountCapability.MOUNT_AS_DRIVE_LETTER, MountCapability.MOUNT_TO_SYSTEM_CHOSEN_PATH, MountCapability.UNMOUNT_FORCED, MountCapability.VOLUME_ID, MountCapability.VOLUME_NAME);
	}

	@Override
	public int getDefaultLoopbackPort() {
		return 0; //mounts to a drive letter, hence fixed port is not necessary
	}

	@Override
	public MountBuilder forFileSystem(Path path) {
		return new MountBuilderImpl(path);
	}

	private static class MountBuilderImpl extends AbstractMountBuilder {

		private Path driveLetter;
		private String hostName;
		private String volumeName;

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

		@SuppressWarnings("ResultOfMethodCallIgnored")
		@Override
		public MountBuilder setLoopbackHostName(String hostName) {
			this.hostName = hostName;
			try {
				Path.of("\\\\" + hostName + "\\share");
				new URL("http", hostName, 80, "/");
			} catch (MalformedURLException | InvalidPathException e) {
				throw new IllegalArgumentException("hostName \"" + hostName + "\" does not satifsfy OS restrictions.", e);
			}
			return this;
		}

		@Override
		public MountBuilder setVolumeName(String volumeName) {
			this.volumeName = volumeName;
			return this;
		}

		@Override
		protected String getContextPath() {
			return super.getContextPath() + "/" + volumeName;
		}


		@Override
		protected Mount mount(WebDavServerHandle serverHandle, WebDavServletController servlet, URI uri) throws MountFailedException {
			try {
				tuneProxyConfigSilently(uri);
				String mountPoint = driveLetter == null //
						? SYSTEM_CHOSEN_MOUNTPOINT // MOUNT_TO_SYSTEM_CHOSEN_PATH
						: driveLetter.toString().substring(0, 2); // MOUNT_AS_DRIVE_LETTER
				String uncPath = "\\\\" + (hostName == null ? uri.getHost() : hostName) + "@" + uri.getPort() + uri.getRawPath().replace('/', '\\');
				ProcessBuilder mount = new ProcessBuilder("net", "use", mountPoint, uncPath, "/persistent:no");
				Process mountProcess = mount.start();
				ProcessUtil.waitFor(mountProcess, 30, TimeUnit.SECONDS);
				ProcessUtil.assertExitValue(mountProcess, 0);

				String actualMountpoint;
				if (SYSTEM_CHOSEN_MOUNTPOINT.equals(mountPoint)) {
					@SuppressWarnings("resource") String stdout = mountProcess.inputReader(StandardCharsets.UTF_8).lines().collect(Collectors.joining("\n"));
					actualMountpoint = parseSystemChosenMountpoin(stdout);
				} else {
					actualMountpoint = mountPoint;
				}

				LOG.debug("Mounted {} on drive {}", uncPath, actualMountpoint);
				return new MountImpl(serverHandle, servlet, actualMountpoint, uncPath);
			} catch (IOException | TimeoutException e) {
				throw new MountFailedException(e);
			}

		}

	}

	/**
	 * Extracts the drive letter used as the mountpoint from the output of `net use` process.
	 * <p>
	 * Example output of {@code net use * \\localhost\DavWWWRoot\example} is:
	 * <pre>
	 * Drive Z: is now connected to \\localhost\example.
	 *
	 * The command completed successfully.
	 *
	 * </pre>
	 *
	 * @param processOutput The complete output of the mounting command `net use`
	 * @return The drive letter the filesystem is mounted to.
	 */
	private static String parseSystemChosenMountpoin(String processOutput) {
		Pattern driveLetterPattern = Pattern.compile("\s([A-Z]:)\s");
		Matcher m = driveLetterPattern.matcher(processOutput.trim());
		if (!m.find()) {
			throw new IllegalStateException("Output of `net use` must contain the drive letter");
		}
		return m.group(1);
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
	 * @throws IOException      If registry access fails
	 * @throws TimeoutException If registry access does not finish in time
	 * @deprecated TODO overheadhunter: check if this is really necessary.
	 */
	@Deprecated
	private static void tuneProxyConfig(URI uri) throws IOException, TimeoutException {
		// get existing value for ProxyOverride key from reqistry:
		ProcessBuilder regQuery = new ProcessBuilder("reg", "query", "\"HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings\"", "/v", "ProxyOverride");
		Process regQueryProcess = ProcessUtil.startAndWaitFor(regQuery, 5, TimeUnit.SECONDS);
		@SuppressWarnings("resource") String regQueryResult = regQueryProcess.inputReader(StandardCharsets.UTF_8).lines().collect(Collectors.joining("\n"));

		// determine new value for ProxyOverride key:
		Set<String> overrides = new HashSet<>();
		Matcher matcher = REG_QUERY_PROXY_OVERRIDES_PATTERN.matcher(regQueryResult);
		if (regQueryProcess.exitValue() == 0 && matcher.find()) {
			String originalOverrides = matcher.group(1);
			LOG.debug("Original Registry value for ProxyOverride is: {}", originalOverrides);
			overrides.addAll(Arrays.asList(originalOverrides.split(";")));
		}
		overrides.removeIf(s -> s.startsWith(uri.getHost() + ":"));
		overrides.add("<local>");
		overrides.add(uri.getHost());
		overrides.add(uri.getHost() + ":" + uri.getPort());

		// set new value:
		String adjustedOverrides = String.join(";", overrides);
		ProcessBuilder regAdd = new ProcessBuilder("reg", "add", "\"HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings\"", "/v", "ProxyOverride", "/d", "\"" + adjustedOverrides + "\"", "/f");
		LOG.debug("Setting Registry value for ProxyOverride to: {}", adjustedOverrides);
		Process regAddProcess = ProcessUtil.startAndWaitFor(regAdd, 5, TimeUnit.SECONDS);
		ProcessUtil.assertExitValue(regAddProcess, 0);
	}

	private static class MountImpl extends AbstractMount {

		private final ProcessBuilder unmountCommand;
		private final ProcessBuilder forcedUnmountCommand;

		private final Path mountpoint;
		private final String uncPath;
		private final AtomicBoolean isUnmounted;

		public MountImpl(WebDavServerHandle serverHandle, WebDavServletController servlet, String driveLetter, String uncPath) {
			super(serverHandle, servlet);
			this.unmountCommand = new ProcessBuilder("net", "use", driveLetter, "/delete", "/no");
			this.forcedUnmountCommand = new ProcessBuilder("net", "use", driveLetter, "/delete", "/yes");
			this.mountpoint = Path.of(driveLetter + "\\");
			this.uncPath = uncPath;
			this.isUnmounted = new AtomicBoolean(false);
		}

		@Override
		public Mountpoint getMountpoint() {
			return Mountpoint.forPath(mountpoint);
		}

		@Override
		public void unmount() throws UnmountFailedException {
			unmount(unmountCommand);
		}

		@Override
		public void unmountForced() throws UnmountFailedException {
			unmount(forcedUnmountCommand);
		}

		private synchronized void unmount(ProcessBuilder command) throws UnmountFailedException {
			if (isUnmounted.get()) {
				return;
			}

			try {
				if (!isUnmounted()) {
					ProcessUtil.assertExitValue(ProcessUtil.startAndWaitFor(command, 5, TimeUnit.SECONDS), 0);
				}
				super.unmount();
				isUnmounted.set(true);
			} catch (IOException | TimeoutException e) {
				throw new UnmountFailedException(e);
			}
		}

		/**
		 * Checks, if the webdav servlet is <em>unmounted</em> by parsing output of `net use`.
		 * <p>
		 * As an example, if a network drive with uncPath {@code \\localhost@8080\DavWWWRoot\example} is mounted to Z:, the output of `net use`:
		 * <pre>
		 *  New connections will not be remembered.
		 *
		 *
		 *  Status       Local     Remote                    Network
		 *
		 *  -------------------------------------------------------------------------------
		 *               Z:        \\localhost@8080\example     Web Client Network
		 *  The command completed successfully.
		 * </pre>
		 * Therefore, this method will return {@code false}.
		 *
		 * @return true, if the path of the webdav servlet is not found in the output of `net use`. false if either the path was found or the process exited abnormally
		 */
		@SuppressWarnings("resource")
		private boolean isUnmounted() {
			try {
				ProcessBuilder determineMP = new ProcessBuilder("net", "use");
				Process determineMPProcess = ProcessUtil.startAndWaitFor(determineMP, 5, TimeUnit.SECONDS);
				ProcessUtil.assertExitValue(determineMPProcess, 0);

				return determineMPProcess.inputReader(StandardCharsets.UTF_8).lines().noneMatch(l -> l.contains(uncPath));
			} catch (IOException | TimeoutException e) {
				return false;
			}
		}
	}
}
