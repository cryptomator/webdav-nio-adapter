/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE.txt.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.frontend.webdav.servlet;

import java.util.HashSet;

import org.apache.jackrabbit.webdav.DavSession;

class DavSessionImpl implements DavSession {
	
	private final HashSet<String> lockTokens = new HashSet<String>();
	private final HashSet<Object> references = new HashSet<Object>();

	@Override
	public void addReference(Object reference) {
		references.add(reference);
	}

	@Override
	public void removeReference(Object reference) {
		references.remove(reference);
	}

	@Override
	public void addLockToken(String token) {
		lockTokens.add(token);
	}

	@Override
	public String[] getLockTokens() {
		return lockTokens.toArray(new String[lockTokens.size()]);
	}

	@Override
	public void removeLockToken(String token) {
		lockTokens.remove(token);
	}

}
