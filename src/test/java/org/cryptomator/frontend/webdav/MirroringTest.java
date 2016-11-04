/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE.txt.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.frontend.webdav;

import java.io.IOException;
import java.nio.file.Paths;

public class MirroringTest {

	public static void main(String[] args) throws IOException {
		WebDavServerComponent comp = DaggerWebDavServerComponent.create();
		WebDavServer server = comp.server();
		server.setPort(8080);
		server.start();
		server.create(Paths.get("/Users/sebastian/Desktop/ant-javafx"), "test");
		System.out.println("Sytem.in.read() to shutdown ;-)");
		System.in.read();
		server.stop();
	}

}
