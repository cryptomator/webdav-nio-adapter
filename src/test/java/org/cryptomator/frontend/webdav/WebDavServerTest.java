package org.cryptomator.frontend.webdav;

import org.junit.Assert;
import org.junit.Test;

public class WebDavServerTest {

	@Test
	public void testConstructionOfMultipleInstances() {
		WebDavServer server1 = WebDavServer.create("localhost", 0);
		WebDavServer server2 = WebDavServer.create("localhost", 0);
		Assert.assertNotSame(server1, server2);
	}

}
