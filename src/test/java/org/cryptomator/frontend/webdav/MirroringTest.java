/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE.txt.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.frontend.webdav;

import org.cryptomator.integrations.mount.MountFailedException;
import org.cryptomator.integrations.mount.MountFeature;
import org.cryptomator.integrations.mount.MountProvider;
import org.cryptomator.integrations.mount.UnmountFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class MirroringTest {

	static {
		System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info");
		System.setProperty("org.slf4j.simpleLogger.log.org.cryptomator.frontend.webdav", "debug");
		System.setProperty("org.slf4j.simpleLogger.showDateTime", "true");
		System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", "HH:mm:ss.SSS");
	}

	private static final Logger LOG = LoggerFactory.getLogger(MirroringTest.class);

	public static void main(String[] args) throws MountFailedException, IOException {
		var mountProvider = MountProvider.get().findAny().orElseThrow(() -> new MountFailedException("Did not find a mount provider"));
		LOG.info("Using mount provider: {}", mountProvider.displayName());

		try (Scanner scanner = new Scanner(System.in)) {
			LOG.info("Enter path to the directory you want to be accessible via WebDAV:");
			Path pathToMirror = Paths.get(scanner.nextLine());
			if (!Files.isDirectory(pathToMirror)) {
				LOG.error("Invalid directory.");
				System.exit(1);
			}

			var mountBuilder = mountProvider.forFileSystem(pathToMirror);
			if (mountProvider.supportsFeature(MountFeature.PORT)) {
				mountBuilder.setPort(8080);
			}

			try (var mount = mountBuilder.mount()) {
				LOG.info("Enter anything to unmount...");
				System.in.read();

				try {
					mount.unmount();
					LOG.info("Gracefully unmounted.");
				} catch (UnmountFailedException e) {
					if (mountProvider.supportsFeature(MountFeature.UNMOUNT_FORCED)) {
						LOG.warn("Graceful unmount failed. Attempting force-unmount...");
						mount.unmountForced();
						LOG.info("Forcefully unmounted.");
					}
				}
			} catch (UnmountFailedException e) {
				LOG.warn("Unmount failed.", e);
			}
		}
	}

}
