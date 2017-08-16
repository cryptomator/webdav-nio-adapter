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

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cryptomator.frontend.webdav.WebDavServerModule.ContextPaths;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;

@Singleton
class DefaultServlet extends HttpServlet {

	private static final String METHOD_PROPFIND = "PROPFIND";
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
				Thread.sleep(TARPIT_DELAY_MS);
				resp.addHeader("X-Tarpit-Delayed", TARPIT_DELAY_MS + "ms");
				resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			} catch (InterruptedException e) {
				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Thread interrupted.");
				Thread.currentThread().interrupt();
			}
			return;
		}

		switch (req.getMethod()) {
		case METHOD_PROPFIND:
			doPropfind(req, resp);
			break;
		default:
			super.service(req, resp);
			break;
		}
	}

	@Override
	protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.addHeader("DAV", "1, 2");
		resp.addHeader("MS-Author-Via", "DAV");
		resp.addHeader("Allow", "OPTIONS, PROPFIND, GET, HEAD");
		resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
	}

	protected void doPropfind(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.getWriter()
				.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" //
						+ "<D:multistatus xmlns:D=\"DAV:\">\n" //
						+ "<D:response>\n" //
						+ "  <D:href>" + req.getRequestURI() + "</D:href>\n" //
						+ "  <D:propstat>\n" //
						+ "    <D:prop>\n" //
						+ "      <D:iscollection>1</D:iscollection>\n" //
						+ "      <D:resourcetype><D:collection/></D:resourcetype>\n" //
						+ "    </D:prop>\n" //
						+ "  </D:propstat>\n" //
						+ "</D:response>\n" //
						+ "</D:multistatus>");
		resp.getWriter().flush();
	}

	private boolean isRequestedResourcePathPartOfValidContextPath(String requestedResourcePath) {
		return contextPaths.stream().filter(cp -> isParentOrSamePath(cp, requestedResourcePath)).findAny().isPresent();
	}

	private boolean isParentOrSamePath(String path, String potentialParent) {
		String[] pathComponents = Iterables.toArray(Splitter.on('/').omitEmptyStrings().split(path), String.class);
		String[] parentPathComponents = Iterables.toArray(Splitter.on('/').omitEmptyStrings().split(potentialParent), String.class);
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
