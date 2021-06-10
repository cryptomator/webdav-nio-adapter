module org.cryptomator.frontend.webdav {
	requires org.cryptomator.frontend.webdav.servlet;
	requires org.eclipse.jetty.server;
	requires org.eclipse.jetty.servlet;
	requires com.google.common;
	requires org.slf4j;

	/* TODO: filename-based modules: */
	requires dagger;
	requires static javax.inject;

	exports org.cryptomator.frontend.webdav;
	exports org.cryptomator.frontend.webdav.mount;
	exports org.cryptomator.frontend.webdav.servlet;
}