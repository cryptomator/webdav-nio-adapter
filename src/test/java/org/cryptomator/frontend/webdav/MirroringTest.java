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
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.cryptomator.frontend.webdav.mount.Mounter.CommandFailedException;
import org.cryptomator.frontend.webdav.mount.Mounter.Mount;
import org.cryptomator.frontend.webdav.mount.Mounter.MountParam;
import org.cryptomator.frontend.webdav.servlet.WebDavServletController;

public class MirroringTest {

	public static void main(String[] args) throws IOException, CommandFailedException {
		try (Scanner scanner = new Scanner(System.in)) {
			System.out.println("Enter path to the directory you want to be accessible via WebDAV:");
			Path p = Paths.get(scanner.nextLine());
			if (Files.isDirectory(p)) {

				// self-sigend:
				// openssl genrsa -aes128 -out jetty.key
				// openssl req -new -x509 -newkey rsa:2048 -sha256 -key jetty.key -out jetty.crt
				// openssl pkcs12 -inkey jetty.key -in jetty.crt -export -out jetty.pkcs12
				WebDavServer server = WebDavServer.create("/Users/sebastian/Desktop/jetty.pkcs12", "test");
				server.bind("localhost", 8080);
				server.start();
				WebDavServletController servlet = server.createWebDavServlet(p, "test");
				servlet.start();

				Map<MountParam, String> mountOptions = new HashMap<>();
				mountOptions.put(MountParam.WIN_DRIVE_LETTER, "X:");
				mountOptions.put(MountParam.PREFERRED_GVFS_SCHEME, "dav");
				mountOptions.put(MountParam.UNIQUE_VAULT_ID, "MirroringTest");
				Mount mount = servlet.mount(mountOptions);
				mount.reveal();

				System.out.println("Enter anything to stop the server...");
				System.in.read();
				mount.unmount();
				server.stop();
			} else {
				System.out.println("Invalid directory.");
				return;
			}
		}
	}

}
