package org.cryptomator.frontend.webdav.mount;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.TimeUnit;

abstract class VfsMountingStrategy implements MounterStrategy {

	protected abstract class MountImpl implements Mount {

		ProcessBuilder revealCmd;
		ProcessBuilder isMountedCmd;
		ProcessBuilder unmountCmd;

		protected URI uri;

		@Override
		public void unmount() throws CommandFailedException {
			Process isMountedProcess = ProcessUtil.startAndWaitFor(isMountedCmd, 5, TimeUnit.SECONDS);
			if (isMountedProcess.exitValue() == 0) {
				// only unmount if volume is still mounted, noop otherwise
				ProcessUtil.assertExitValue(ProcessUtil.startAndWaitFor(unmountCmd, 5, TimeUnit.SECONDS), 0);
			}
		}

		@Override
		public URL getURLofWebDAVDirectory() {
			try {
				return uri.toURL();
			} catch (MalformedURLException e) {
				return null; // should not happen
			}
		}

		@Override
		public void reveal() throws CommandFailedException {
			ProcessUtil.startAndWaitFor(revealCmd, 5, TimeUnit.SECONDS);
		}

		@Override
		public void reveal(Revealer revealer) throws RevealException {
			try {
				reveal();
			} catch (CommandFailedException e) {
				throw new RevealException(e);
			}
		}
	}
}
