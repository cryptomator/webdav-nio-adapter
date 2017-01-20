package org.cryptomator.frontend.webdav.mount;

import java.net.URI;
import java.util.Map;

public interface Mounter {

	/**
	 * Tries to mount a given webdav share.
	 * 
	 * @param uri URI of the webdav share
	 * @param mountParams additional mount parameters, that might be needed by certain OS-specific mount commands.
	 * @return a {@link Mount} representing the mounted share
	 * @throws CommandFailedException if the mount operation fails
	 * @throws IllegalArgumentException if mountParams is missing expected options
	 */
	Mount mount(URI uri, Map<MountParam, String> mountParams) throws CommandFailedException;

	public enum MountParam {
		@Deprecated MOUNT_NAME, //
		WIN_DRIVE_LETTER, //
		PREFERRED_GVFS_SCHEME;
	}

	/**
	 * Represents a single mounted volume and allows certain interactions with it.
	 */
	public interface Mount {
		void unmount() throws CommandFailedException;

		void reveal() throws CommandFailedException;
	}

	public class CommandFailedException extends Exception {

		public CommandFailedException(String message) {
			super(message);
		}

		public CommandFailedException(Throwable cause) {
			super(cause);
		}

	}

}
