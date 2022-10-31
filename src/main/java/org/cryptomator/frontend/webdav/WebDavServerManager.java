package org.cryptomator.frontend.webdav;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class WebDavServerManager {

	private static final ConcurrentMap<Integer, ReferenceCountingHandle> RUNNING_SERVERS = new ConcurrentHashMap<>();

	private WebDavServerManager() {
	}

	public static WebDavServerHandle getOrCreateServer(int port) throws ServerLifecycleException {
		return RUNNING_SERVERS.compute(port, (p, handle) -> {
			if (handle == null) {
				var server = tryCreate(p);
				return new ReferenceCountingHandle(port, server, new AtomicInteger(1));
			} else {
				handle.counter.incrementAndGet();
				return handle;
			}
		});
	}

	private static WebDavServer tryCreate(int port) throws ServerLifecycleException {
		var bindAddr = new InetSocketAddress(InetAddress.getLoopbackAddress(), port);
		var server = WebDavServerFactory.createWebDavServer(bindAddr);
		server.start();
		return server;
	}

	private record ReferenceCountingHandle(int port, WebDavServer server, AtomicInteger counter) implements WebDavServerHandle {

		@Override
		public void close() {
			if (counter.decrementAndGet() == 0) {
				RUNNING_SERVERS.remove(port, server);
				server.terminate();
			}
		}

	}

}
