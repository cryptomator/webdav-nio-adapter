package org.cryptomator.frontend.webdav.mount;

import java.util.Set;

public class MounterModule {

	public static Mounter findMounter() {
		FallbackMounter fallback = new FallbackMounter();
		Set<MounterStrategy> strategies = Set.of(
				new WindowsMounter(),
				new MacAppleScriptMounter(),
				new MacShellScriptMounter(),
				new LinuxGioMounter(),
				new LinuxGvfsMounter()
		);
		return strategies.stream().filter(MounterStrategy::isApplicable).findFirst().orElse(fallback);
	}

}
