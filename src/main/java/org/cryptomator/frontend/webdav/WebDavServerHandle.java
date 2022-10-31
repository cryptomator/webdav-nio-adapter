package org.cryptomator.frontend.webdav;

public interface WebDavServerHandle extends AutoCloseable {

	WebDavServer server();

	@Override
	void close();
}
