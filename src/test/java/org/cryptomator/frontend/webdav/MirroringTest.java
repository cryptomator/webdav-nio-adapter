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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class MirroringTest {

	public static void main(String[] args) throws IOException {
		try (Scanner scanner = new Scanner(System.in)) {
			System.out.println("Enter path to the directory you want to be accessible via WebDAV:");
			Path p = Paths.get(scanner.nextLine());
			if (Files.isDirectory(p)) {
				WebDavServer server = WebDavServer.create(8080);
				server.start();
				server.startWebDavServlet(p, "test");
				System.out.println("Enter anything to stop the server...");
				System.in.read();
				server.stop();
			} else {
				System.out.println("Invalid directory.");
				return;
			}
		}
	}

}
