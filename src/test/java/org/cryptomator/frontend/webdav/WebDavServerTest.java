package org.cryptomator.frontend.webdav;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class WebDavServerTest {

	@Test
	public void testConstructionOfMultipleInstances() {
		WebDavServer server1 = WebDavServer.create();
		WebDavServer server2 = WebDavServer.create();
		Assertions.assertNotSame(server1, server2);
	}

}
