/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE.txt.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.frontend.webdav.servlet;

import javax.inject.Inject;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.DavSessionProvider;
import org.apache.jackrabbit.webdav.WebdavRequest;

@PerServlet
class DavSessionProviderImpl implements DavSessionProvider {

	@Inject
	public DavSessionProviderImpl() {
	}

	@Override
	public boolean attachSession(WebdavRequest request) throws DavException {
		// every request gets a new session
		final DavSession session = new DavSessionImpl();
		session.addReference(request);
		request.setDavSession(session);
		return true;
	}

	@Override
	public void releaseSession(WebdavRequest request) {
		final DavSession session = request.getDavSession();
		if (session != null) {
			session.removeReference(request);
			request.setDavSession(null);
		}
	}

}
