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
import java.util.Collection;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.cryptomator.frontend.webdav.WebDavServerModule.ContextPaths;

@Singleton
class DefaultServlet extends HttpServlet {

	private static final int TARPIT_DELAY_MS = 5000;
	private final Collection<String> contextPaths;

	@Inject
	public DefaultServlet(@ContextPaths Collection<String> contextPaths) {
		this.contextPaths = contextPaths;
	}

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (!isRequestedResourcePathPartOfValidContextPath(req.getRequestURI())) {
			try {
				resp.addHeader("X-Tarpit-Delayed", TARPIT_DELAY_MS + "ms");
				Thread.sleep(TARPIT_DELAY_MS);
			} catch (InterruptedException e) {
				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Thread interrupted.");
				Thread.currentThread().interrupt();
				return;
			}
		}
		super.service(req, resp);
	}

	@Override
	protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.addHeader("DAV", "1, 2");
		resp.addHeader("MS-Author-Via", "DAV");
		resp.addHeader("Allow", "OPTIONS, GET, HEAD");
		resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
	}

	private boolean isRequestedResourcePathPartOfValidContextPath(String requestedResourcePath) {
		return contextPaths.stream().filter(cp -> isParentOrSamePath(cp, requestedResourcePath)).findAny().isPresent();
	}

	private boolean isParentOrSamePath(String path, String potentialParent) {
		String[] pathComponents = StringUtils.split(Objects.requireNonNull(path), '/');
		String[] parentPathComponents = StringUtils.split(Objects.requireNonNull(potentialParent), '/');
		if (pathComponents.length < parentPathComponents.length) {
			return false; // parent can not be parent of path, if it is longer than path.
		}
		assert pathComponents.length >= parentPathComponents.length;
		for (int i = 0; i < parentPathComponents.length; i++) {
			if (!pathComponents[i].equals(parentPathComponents[i])) {
				return false;
			}
		}
		return true;
	}

}
