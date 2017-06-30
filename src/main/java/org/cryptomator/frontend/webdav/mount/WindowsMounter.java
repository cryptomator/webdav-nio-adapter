package org.cryptomator.frontend.webdav.mount;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class WindowsMounter implements MounterStrategy {

	private static final Logger LOG = LoggerFactory.getLogger(WindowsMounter.class);
	private static final String LOCALHOST_ALIAS = "cryptomator-vault";
	private static final Pattern WIN_MOUNT_DRIVELETTER_PATTERN = Pattern.compile("\\s*([A-Z]:)\\s*");
	private static final Pattern REG_QUERY_PROXY_OVERRIDES_PATTERN = Pattern.compile("\\s*ProxyOverride\\s+REG_SZ\\s+(.*)\\s*");
	private static final String AUTOASSIGN_DRRIVE_LETTER = "*";

	@Override
	public boolean isApplicable() {
		return SystemUtils.IS_OS_WINDOWS;
	}

	@Override
	public Mount mount(URI uri, MountParams mountParams) throws CommandFailedException {
		try {
			String host = hasLocalhostAlias() ? LOCALHOST_ALIAS : uri.getHost();
			URI adjustedUri = new URI(uri.getScheme(), uri.getUserInfo(), host, uri.getPort(), uri.getPath(), uri.getQuery(), uri.getFragment());
			return mountInternal(adjustedUri, mountParams);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Unable to reconstruct URI from given URI", e);
		}
	}
	
	private boolean hasLocalhostAlias() {
		try {
			InetAddress alias = InetAddress.getByName(LOCALHOST_ALIAS);
			return alias.getHostAddress().equals("127.0.0.1");
		} catch (UnknownHostException e) {
			return false;
		}
	}

	private Mount mountInternal(URI uri, Map<MountParam, String> mountParams) throws CommandFailedException {
		try {
			tuneProxyConfig(uri);
			String preferredDriveLetter = mountParams.getOrDefault(MountParam.WIN_DRIVE_LETTER, AUTOASSIGN_DRRIVE_LETTER);

			String uncPath = "\\\\" + uri.getHost() + "@" + uri.getPort() + "\\DavWWWRoot" + uri.getRawPath().replace('/', '\\');
			ProcessBuilder mount = new ProcessBuilder("net", "use", preferredDriveLetter, uncPath, "/persistent:no");
			Process mountProcess = mount.start();
			String stdout = ProcessUtil.toString(mountProcess.getInputStream(), StandardCharsets.UTF_8);
			ProcessUtil.waitFor(mountProcess, 30, TimeUnit.SECONDS);
			ProcessUtil.assertExitValue(mountProcess, 0);
			String driveLetter = AUTOASSIGN_DRRIVE_LETTER.equals(preferredDriveLetter) ? getDriveLetter(stdout) : preferredDriveLetter;
			LOG.debug("Mounted {} on drive {}", uncPath, driveLetter);
			return new MountImpl(driveLetter);
		} catch (IOException e) {
			throw new CommandFailedException(e);
		}
	}

	private String getDriveLetter(String result) throws CommandFailedException {
		final Matcher matcher = WIN_MOUNT_DRIVELETTER_PATTERN.matcher(result);
		if (matcher.find()) {
			return matcher.group(1);
		} else {
			throw new CommandFailedException("Failed to get a drive letter from net use output.");
		}
	}

	/**
	 * @param uri The URI for which to tune the registry settings
	 * @throws CommandFailedException If registry access fails
	 * @deprecated TODO overheadhunter: check if this is really necessary.
	 */
	@Deprecated
	private void tuneProxyConfig(URI uri) throws CommandFailedException {
		try {
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
				Arrays.stream(StringUtils.split(originalOverrides, ';')).forEach(overrides::add);
			}
			overrides.removeIf(s -> s.startsWith(uri.getHost() + ":"));
			overrides.add("<local>");
			overrides.add(uri.getHost());
			overrides.add(uri.getHost() + ":" + uri.getPort());

			// set new value:
			String adjustedOverrides = StringUtils.join(overrides, ';');
			ProcessBuilder regAdd = new ProcessBuilder("reg", "add", "\"HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings\"", "/v", "ProxyOverride", "/d", "\"" + adjustedOverrides + "\"", "/f");
			LOG.debug("Setting Registry value for ProxyOverride to: {}", adjustedOverrides);
			Process regAddProcess = ProcessUtil.startAndWaitFor(regAdd, 5, TimeUnit.SECONDS);
			ProcessUtil.assertExitValue(regAddProcess, 0);
		} catch (IOException e) {
			throw new CommandFailedException(e);
		}

	}

	private static class MountImpl implements Mount {

		private final ProcessBuilder unmountCommand;
		private final ProcessBuilder forcedUnmountCommand;
		private final ProcessBuilder revealCommand;

		public MountImpl(String driveLetter) {
			this.unmountCommand = new ProcessBuilder("net", "use", driveLetter, "/delete", "/no");
			this.forcedUnmountCommand = new ProcessBuilder("net", "use", driveLetter, "/delete", "/yes");
			this.revealCommand = new ProcessBuilder("explorer.exe", "/root," + driveLetter);
		}

		@Override
		public Optional<UnmountOperation> forced() {
			return Optional.of(() -> run(forcedUnmountCommand));
		}

		@Override
		public void unmount() throws CommandFailedException {
			run(unmountCommand);
		}

		private void run(ProcessBuilder command) throws CommandFailedException {
			ProcessUtil.assertExitValue(ProcessUtil.startAndWaitFor(command, 5, TimeUnit.SECONDS), 0);
		}

		@Override
		public void reveal() throws CommandFailedException {
			ProcessUtil.startAndWaitFor(revealCommand, 5, TimeUnit.SECONDS);
		}

	}

}
