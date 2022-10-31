package org.cryptomator.frontend.webdav.mount;

import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

public interface Mounter {

	static Mounter find() {
		FallbackMounter fallback = new FallbackMounter();
		Set<MounterStrategy> strategies = Set.of(
				new WindowsMounter(),
				new MacAppleScriptMounter(),
				new MacShellScriptMounter(),
				new LinuxGioMounter(),
				new LinuxGvfsMounter()
		);
		return strategies.stream().filter(MounterStrategy::isApplicable).findFirst().orElse(fallback);
	}

	/**
	 * Tries to mount a given webdav share.
	 *
	 * @param uri         URI of the webdav share
	 * @param mountParams additional mount parameters, that might be needed by certain OS-specific mount commands.
	 * @return a {@link Mount} representing the mounted share
	 * @throws CommandFailedException   if the mount operation fails
	 * @throws IllegalArgumentException if mountParams is missing expected options
	 */
	Mount mount(URI uri, MountParams mountParams) throws CommandFailedException;

	/**
	 * Represents a single mounted volume and allows certain interactions with it.
	 */
	interface Mount extends UnmountOperation {

		default Optional<Path> getMountPoint() {
			return Optional.empty();
		}

		URI getWebDavUri();

		default Optional<UnmountOperation> forced() {
			return Optional.empty();
		}

		@Deprecated
		void reveal() throws CommandFailedException;

		void reveal(Revealer revealer) throws Exception;

	}

	interface UnmountOperation {

		void unmount() throws CommandFailedException;

	}

	/**
	 * Thrown if the mount or unmount operation for a webdav directory fails.
	 */
	class CommandFailedException extends Exception {

		public CommandFailedException(String message) {
			super(message);
		}

		public CommandFailedException(Throwable cause) {
			super(cause);
		}

	}

	/**
	 * Thrown when the underlying OS is not supported mounting a webdav directory.
	 * <p>
	 * It contains the uri of the webdav service to offer callers the possiblilty of process it further.
	 */
	class UnsupportedSystemException extends CommandFailedException {

		private static final String MESSAGE = "No applicable mounting strategy found for this system.";
		private final URI uri;

		public UnsupportedSystemException(URI uri) {
			super(MESSAGE);
			this.uri = uri;
		}

		public URI getUri() {
			return uri;
		}

	}

	@FunctionalInterface
	interface Revealer {

		void reveal(Path path) throws Exception;

	}

}
