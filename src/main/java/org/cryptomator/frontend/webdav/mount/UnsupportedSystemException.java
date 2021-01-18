package org.cryptomator.frontend.webdav.mount;

import java.net.URI;

/**
 * Thrown when the underlying OS is not supported mounting a webdav directory.
 * <p>
 * It contains the uri of the webdav service to offer callers the possiblilty of process it further.
 */
public class UnsupportedSystemException extends Exception {

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
