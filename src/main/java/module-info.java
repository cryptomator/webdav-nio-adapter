import org.cryptomator.frontend.webdav.mount.MacAppleScriptMounter;
import org.cryptomator.integrations.mount.MountProvider;

module org.cryptomator.frontend.webdav {
	requires org.cryptomator.frontend.webdav.servlet;
	requires org.cryptomator.integrations.api;
	requires org.eclipse.jetty.server;
	requires org.eclipse.jetty.servlet;
	requires com.google.common;
	requires org.slf4j;
	requires static org.jetbrains.annotations;

	provides MountProvider with MacAppleScriptMounter;
}