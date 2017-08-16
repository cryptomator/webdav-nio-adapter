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
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Qualifier;
import javax.inject.Singleton;

import org.cryptomator.frontend.webdav.mount.MounterModule;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.ExecutorThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;

import dagger.Module;
import dagger.Provides;

@Module(includes = {MounterModule.class})
class WebDavServerModule {

	private static final int MAX_PENDING_REQUESTS = 400;
	private static final int MAX_THREADS = 100;
	private static final int THREAD_IDLE_SECONDS = 60;
	private static final String ROOT_PATH = "/";
	private static final String PKCS12_KEYSTORE_TYPE = "pkcs12";
	private static final String[] WHITELISTED_CIPHER_SUITES = { //
			"TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384", "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256", // EC (RSA keys) + GCM + SHA2
			"TLS_DHE_RSA_WITH_AES_256_GCM_SHA384", "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256", // DHE (RSA keys) + GCM + SHA2
			"TLS_DHE_RSA_WITH_AES_256_CBC_SHA", "TLS_DHE_RSA_WITH_AES_128_CBC_SHA", // DHE (RSA keys) + CBC + SHA
			"TLS_RSA_WITH_AES_256_GCM_SHA384", "TLS_RSA_WITH_AES_128_GCM_SHA256", // RSA + GCM + SHA2
			"TLS_RSA_WITH_AES_256_CBC_SHA256", "TLS_RSA_WITH_AES_128_CBC_SHA256", // RSA + CBC + SHA2
			"TLS_RSA_WITH_AES_256_CBC_SHA", "TLS_RSA_WITH_AES_128_CBC_SHA" // RSA + CBC + SHA1
	};

	private final Optional<String> pkcs12KeystorePath;
	private final Optional<String> pkcs12KeystorePassword;

	WebDavServerModule(Optional<String> pkcs12KeystorePath, Optional<String> pkcs12KeystorePassword) {
		this.pkcs12KeystorePath = pkcs12KeystorePath;
		this.pkcs12KeystorePassword = pkcs12KeystorePassword;
	}

	@Provides
	@Singleton
	ExecutorService provideExecutorService() {
		// set core pool size = MAX_THREADS and allow coreThreadTimeOut to enforce spawning threads till the maximum even if the queue is not full
		BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(MAX_PENDING_REQUESTS);
		AtomicInteger threadNum = new AtomicInteger(1);
		ThreadPoolExecutor executor = new ThreadPoolExecutor(MAX_THREADS, MAX_THREADS, THREAD_IDLE_SECONDS, TimeUnit.SECONDS, queue, r -> {
			Thread t = new Thread(r, String.format("Server thread %03d", threadNum.getAndIncrement()));
			t.setDaemon(true);
			return t;
		});
		executor.allowCoreThreadTimeOut(true);
		return executor;
	}

	@Provides
	@Singleton
	Server provideServer(ExecutorService executor, ContextHandlerCollection servletCollection) {
		ThreadPool threadPool = new ExecutorThreadPool(executor);
		Server server = new Server(threadPool);
		server.unmanage(threadPool); // prevent threadpool from being shutdown when stopping the server
		server.setHandler(servletCollection);
		return server;
	}

	@Provides
	@Singleton
	Optional<SslContextFactory> provideSslContextFactory() {
		if (pkcs12KeystorePath.isPresent() && pkcs12KeystorePassword.isPresent()) {
			SslContextFactory sslContextFactory = new SslContextFactory();
			sslContextFactory.setKeyStorePath(pkcs12KeystorePath.get());
			sslContextFactory.setKeyStorePassword(pkcs12KeystorePassword.get());
			sslContextFactory.setKeyStoreType(PKCS12_KEYSTORE_TYPE);
			sslContextFactory.setIncludeCipherSuites(WHITELISTED_CIPHER_SUITES);
			return Optional.of(sslContextFactory);
		} else {
			return Optional.empty();
		}
	}

	@Provides
	@Singleton
	ServerConnector provideServerConnector(Server server, Optional<SslContextFactory> sslContextFactory) {
		final ServerConnector connector;
		if (sslContextFactory.isPresent()) {
			connector = new ServerConnector(server, sslContextFactory.get());
		} else {
			connector = new ServerConnector(server);
		}
		server.setConnectors(new Connector[] {connector});
		return connector;
	}

	@Provides
	@Singleton
	ContextHandlerCollection provideContextHandlerCollection(@CatchAll ServletContextHandler catchAllServletHandler) {
		ContextHandlerCollection collection = new ContextHandlerCollection();
		collection.addHandler(catchAllServletHandler);
		return collection;
	}

	@Provides
	@Singleton
	@CatchAll
	ServletContextHandler provideServletContextHandler(DefaultServlet servlet) {
		final ServletContextHandler servletContext = new ServletContextHandler(null, ROOT_PATH, ServletContextHandler.NO_SESSIONS);
		final ServletHolder servletHolder = new ServletHolder(ROOT_PATH, servlet);
		servletContext.addServlet(servletHolder, ROOT_PATH);
		return servletContext;
	}

	@Provides
	@Singleton
	@ContextPaths
	Collection<String> provideContextPaths() {
		return new HashSet<>();
	}

	@Qualifier
	@Documented
	@Retention(RetentionPolicy.RUNTIME)
	@interface CatchAll {
	}

	@Qualifier
	@Documented
	@Retention(RetentionPolicy.RUNTIME)
	@interface ContextPaths {
	}

}
