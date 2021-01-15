package org.cryptomator.frontend.webdav.mount;

import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.Optional;

public interface Mounter {

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
	public interface Mount extends UnmountOperation {

		default Optional<Path> getMountPoint() {
			return Optional.empty();
		}

		URL getURLofWebDAVDirectory();

		default Optional<UnmountOperation> forced() {
			return Optional.empty();
		}

		void reveal() throws CommandFailedException;

		void reveal(Revealer revealer) throws RevealException;

	}

	public interface UnmountOperation {

		void unmount() throws CommandFailedException;

	}

	public class CommandFailedException extends Exception {

		public CommandFailedException(String message) {
			super(message);
		}

		public CommandFailedException(Throwable cause) {
			super(cause);
		}

	}

	interface Revealer {
		public void reveal(Path p) throws RevealException;
	}

	class RevealException extends Exception {
		public RevealException(String msg) {
			super(msg);
		}

		public RevealException(Throwable cause) {
			super(cause);
		}

		public RevealException(String msg, Throwable cause) {
			super(msg, cause);
		}
	}
}
