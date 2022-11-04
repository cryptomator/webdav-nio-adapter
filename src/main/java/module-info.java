import org.cryptomator.frontend.webdav.mount.FallbackMounter;
import org.cryptomator.frontend.webdav.mount.LinuxGioMounter;
import org.cryptomator.frontend.webdav.mount.MacAppleScriptMounter;
import org.cryptomator.frontend.webdav.mount.WindowsMounter;
import org.cryptomator.integrations.mount.MountService;

module org.cryptomator.frontend.webdav {
	requires org.cryptomator.frontend.webdav.servlet;
	requires org.cryptomator.integrations.api;
	requires org.eclipse.jetty.server;
	requires org.eclipse.jetty.servlet;
	requires com.google.common;
	requires org.slf4j;
	requires static org.jetbrains.annotations;

	provides MountService with MacAppleScriptMounter, FallbackMounter, WindowsMounter, LinuxGioMounter;
}