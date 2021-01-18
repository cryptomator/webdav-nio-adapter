package org.cryptomator.frontend.webdav.mount;

import java.net.URI;

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
