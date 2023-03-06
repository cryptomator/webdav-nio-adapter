/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE.txt.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.frontend.webdav;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Predicate;
import java.util.regex.Pattern;

class DefaultServlet extends HttpServlet implements ContextPathRegistry {

	private static final String METHOD_PROPFIND = "PROPFIND";
	private static final int TARPIT_DELAY_MS = 5000;
	private static final Pattern PATH_SEP_PATTERN = Pattern.compile("/");
	private final Set<String> contextPaths;

	public DefaultServlet(Set<String> contextPaths) {
		this.contextPaths = new CopyOnWriteArraySet<>(contextPaths);
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
	protected void doOptions(HttpServletRequest req, HttpServletResponse resp) {
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
		return contextPaths.stream().anyMatch(cp -> isParentOrSamePath(cp, requestedResourcePath));
	}

	@Override
	public boolean add(String contextPath) {
		return contextPaths.add(contextPath);
	}

	@Override
	public boolean remove(String contextPath) {
		return contextPaths.remove(contextPath);
	}

	private boolean isParentOrSamePath(String path, String potentialParent) {
		String[] pathComponents = PATH_SEP_PATTERN.splitAsStream(path).filter(Predicate.not(String::isBlank)).toArray(String[]::new);
		String[] parentPathComponents = PATH_SEP_PATTERN.splitAsStream(potentialParent).filter(Predicate.not(String::isBlank)).toArray(String[]::new);
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
