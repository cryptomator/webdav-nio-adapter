module org.cryptomator.frontend.webdav {
	requires org.cryptomator.frontend.webdav.servlet;
	requires org.eclipse.jetty.server;
	requires org.eclipse.jetty.servlet;
	requires com.google.common;
	requires org.slf4j;
	requires dagger;

	// filename-based module required by dagger
	// we will probably need to live with this for a while:
	// https://github.com/javax-inject/javax-inject/issues/33
	// May be provided by another lib during runtime
	requires static javax.inject;

	exports org.cryptomator.frontend.webdav;
	exports org.cryptomator.frontend.webdav.mount;
	exports org.cryptomator.frontend.webdav.servlet;
}