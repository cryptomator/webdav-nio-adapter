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

import javax.inject.Inject;
import javax.servlet.ServletException;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavLocatorFactory;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.DavSessionProvider;
import org.apache.jackrabbit.webdav.WebdavRequest;
import org.apache.jackrabbit.webdav.WebdavResponse;
import org.apache.jackrabbit.webdav.header.IfHeader;
import org.apache.jackrabbit.webdav.lock.ActiveLock;
import org.apache.jackrabbit.webdav.lock.Scope;
import org.apache.jackrabbit.webdav.lock.Type;
import org.apache.jackrabbit.webdav.server.AbstractWebdavServlet;
import org.eclipse.jetty.io.EofException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterators;

@PerServlet
public class WebDavServlet extends AbstractWebdavServlet {

	private static final String NO_LOCK = "DAV:no-lock";
	private static final Logger LOG = LoggerFactory.getLogger(WebDavServlet.class);

	private final DavSessionProvider davSessionProvider;
	private final DavLocatorFactory davLocatorFactory;
	private final DavResourceFactory davResourceFactory;

	@Inject
	public WebDavServlet(DavSessionProviderImpl davSessionProvider, DavLocatorFactoryImpl davLocatorFactory, DavResourceFactoryImpl davResourceFactory) {
		this.davSessionProvider = davSessionProvider;
		this.davLocatorFactory = davLocatorFactory;
		this.davResourceFactory = davResourceFactory;
	}

	@Override
	protected boolean isPreconditionValid(WebdavRequest request, DavResource resource) {
		IfHeader ifHeader = new IfHeader(request);
		if (ifHeader.hasValue() && Iterators.all(ifHeader.getAllTokens(), Predicates.equalTo(NO_LOCK))) {
			// https://tools.ietf.org/html/rfc4918#section-10.4.8:
			// "DAV:no-lock" is known to never represent a current lock token.
			return false;
		} else if (ifHeader.hasValue() && Iterators.any(ifHeader.getAllNotTokens(), Predicates.equalTo(NO_LOCK))) {
			// by applying "Not" to a state token that is known not to be current, the Condition always evaluates to true.
			return true;
		} else {
			return request.matchesIfHeader(resource);
		}
	}

	@Override
	public DavSessionProvider getDavSessionProvider() {
		return davSessionProvider;
	}

	@Override
	public void setDavSessionProvider(DavSessionProvider davSessionProvider) {
		throw new UnsupportedOperationException("Setting davSessionProvider not supported.");
	}

	@Override
	public DavLocatorFactory getLocatorFactory() {
		return davLocatorFactory;
	}

	@Override
	public void setLocatorFactory(DavLocatorFactory locatorFactory) {
		throw new UnsupportedOperationException("Setting locatorFactory not supported.");
	}

	@Override
	public DavResourceFactory getResourceFactory() {
		return davResourceFactory;
	}

	@Override
	public void setResourceFactory(DavResourceFactory resourceFactory) {
		throw new UnsupportedOperationException("Setting resourceFactory not supported.");
	}

	/* Unchecked DAV exception rewrapping */

	@Override
	protected boolean execute(WebdavRequest request, WebdavResponse response, int method, DavResource resource) throws ServletException, IOException, DavException {
		try {
			return super.execute(request, response, method, resource);
		} catch (UncheckedDavException e) {
			throw e.toDavException();
		}
	}

	/* GET stuff */

	@Override
	protected void doGet(WebdavRequest request, WebdavResponse response, DavResource resource) throws IOException, DavException {
		try {
			super.doGet(request, response, resource);
		} catch (EofException e) {
			// Jetty EOF (other than IO EOF) is thrown when the connection is closed by the client.
			// If the client is no longer interested in further content, we don't care.
			if (LOG.isDebugEnabled()) {
				LOG.trace("Unexpected end of stream during GET (client hung up).");
			}
		}
	}

	/* LOCK stuff */

	@Override
	protected int validateDestination(DavResource destResource, WebdavRequest request, boolean checkHeader) throws DavException {
		if (isLocked(destResource) && !hasCorrectLockTokens(request.getDavSession(), destResource)) {
			throw new DavException(DavServletResponse.SC_LOCKED, "The destination resource is locked");
		}
		return super.validateDestination(destResource, request, checkHeader);
	}

	@Override
	protected void doPut(WebdavRequest request, WebdavResponse response, DavResource resource) throws IOException, DavException {
		if (isLocked(resource) && !hasCorrectLockTokens(request.getDavSession(), resource)) {
			throw new DavException(DavServletResponse.SC_LOCKED, "The resource is locked");
		}
		super.doPut(request, response, resource);
	}

	@Override
	protected void doDelete(WebdavRequest request, WebdavResponse response, DavResource resource) throws IOException, DavException {
		if (isLocked(resource) && !hasCorrectLockTokens(request.getDavSession(), resource)) {
			throw new DavException(DavServletResponse.SC_LOCKED, "The resource is locked");
		}
		super.doDelete(request, response, resource);
	}

	@Override
	protected void doMove(WebdavRequest request, WebdavResponse response, DavResource resource) throws IOException, DavException {
		if (isLocked(resource) && !hasCorrectLockTokens(request.getDavSession(), resource)) {
			throw new DavException(DavServletResponse.SC_LOCKED, "The source resource is locked");
		}
		super.doMove(request, response, resource);
	}

	@Override
	protected void doPropPatch(WebdavRequest request, WebdavResponse response, DavResource resource) throws IOException, DavException {
		if (isLocked(resource) && !hasCorrectLockTokens(request.getDavSession(), resource)) {
			throw new DavException(DavServletResponse.SC_LOCKED, "The resource is locked");
		}
		super.doPropPatch(request, response, resource);
	}

	private boolean hasCorrectLockTokens(DavSession session, DavResource resource) {
		boolean access = false;
		final String[] providedLockTokens = session.getLockTokens();
		for (ActiveLock lock : resource.getLocks()) {
			access |= ArrayUtils.contains(providedLockTokens, lock.getToken());
		}
		return access;
	}

	private boolean isLocked(DavResource resource) {
		return resource.hasLock(Type.WRITE, Scope.EXCLUSIVE) || resource.hasLock(Type.WRITE, Scope.SHARED);
	}

}
