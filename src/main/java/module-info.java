module org.cryptomator.frontend.webdav {
	requires org.cryptomator.frontend.webdav.servlet;
	requires org.eclipse.jetty.server;
	requires org.eclipse.jetty.servlet;
	requires com.google.common;
	requires org.slf4j;

	exports org.cryptomator.frontend.webdav;
	exports org.cryptomator.frontend.webdav.mount;
	exports org.cryptomator.frontend.webdav.servlet;
}