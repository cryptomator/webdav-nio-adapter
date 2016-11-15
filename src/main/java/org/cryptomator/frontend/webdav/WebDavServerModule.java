/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE.txt.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.frontend.webdav;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.inject.Qualifier;

import dagger.Module;
import dagger.Provides;

@Module
class WebDavServerModule {

	private final int port;

	/**
	 * @param port Fixed TCP server port or 0 to let the OS auto-assign a port.
	 */
	public WebDavServerModule(int port) {
		this.port = port;
	}

	@Provides
	@ServerPort
	int providePort() {
		return port;
	}

	@Qualifier
	@Documented
	@Retention(RetentionPolicy.RUNTIME)
	public @interface ServerPort {
	}

}
