/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE.txt.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.frontend.webdav.servlet;

import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

class CopyingFileVisitor extends SimpleFileVisitor<Path> {

	private final Path srcDir;
	private final Path dstDir;
	private final CopyOption[] options;

	public CopyingFileVisitor(Path srcDir, Path dstDir, CopyOption... options) {
		this.srcDir = srcDir;
		this.dstDir = dstDir;
		this.options = options;
	}

	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
		Path relativePath = srcDir.relativize(dir);
		Path resolvedPath = dstDir.resolve(relativePath);
		Files.copy(dir, resolvedPath, options);
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		Path relativePath = srcDir.relativize(file);
		Path resolvedPath = dstDir.resolve(relativePath);
		Files.copy(file, resolvedPath, options);
		return FileVisitResult.CONTINUE;
	}

}
