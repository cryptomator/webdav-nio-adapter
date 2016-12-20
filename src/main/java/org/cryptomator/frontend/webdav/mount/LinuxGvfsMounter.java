package org.cryptomator.frontend.webdav.mount;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.SystemUtils;

@Singleton
class LinuxGvfsMounter implements MounterStrategy {

	@Inject
	LinuxGvfsMounter() {
	}

	@Override
	public boolean isApplicable() {
		if (!SystemUtils.IS_OS_LINUX) {
			return false;
		}

		// check if gvfs is installed:
		assert SystemUtils.IS_OS_LINUX;
		try {
			ProcessBuilder checkDependencies = new ProcessBuilder("which", "gvfs-mount", "xdg-open");
			Process checkDependenciesProcess = checkDependencies.start();
			ProcessUtil.assertExitValue(checkDependenciesProcess, 0);
			return true;
		} catch (CommandFailedException | IOException e) {
			return false;
		}
	}

	@Override
	public Mount mount(URI uri, Map<MountParam, String> mountParams) throws CommandFailedException {
		// TODO Auto-generated method stub
		throw new CommandFailedException("not yet implemented.");
	}

}
