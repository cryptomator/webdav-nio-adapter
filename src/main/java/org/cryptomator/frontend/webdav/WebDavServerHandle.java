package org.cryptomator.frontend.webdav;

import java.io.IOException;

public interface WebDavServerHandle extends AutoCloseable {

	WebDavServer server();

	@Override
	void close() throws IOException;
}
