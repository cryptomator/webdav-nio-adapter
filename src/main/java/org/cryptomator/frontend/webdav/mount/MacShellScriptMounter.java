package org.cryptomator.frontend.webdav.mount;

import java.net.URI;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.SystemUtils;

@Singleton
class MacShellScriptMounter implements MounterStrategy {

	@Inject
	MacShellScriptMounter() {
	}

	@Override
	public boolean isApplicable() {
		return SystemUtils.IS_OS_MAC_OSX_MAVERICKS || SystemUtils.IS_OS_MAC_OSX_MOUNTAIN_LION;
	}

	@Override
	public Mount mount(URI uri, Map<MountParam, String> mountParams) throws CommandFailedException {
		// TODO Auto-generated method stub
		throw new CommandFailedException("not yet implemented.");
	}

}
