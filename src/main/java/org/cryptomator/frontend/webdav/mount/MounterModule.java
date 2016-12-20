package org.cryptomator.frontend.webdav.mount;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class MounterModule {

	@Provides
	@Singleton
	public Mounter providesMounter(FallbackMounter fallback, WindowsMounter win, MacAppleScriptMounter macAppleScript, MacShellScriptMounter macShellScript, LinuxGvfsMounter linuxGvfs) {
		List<MounterStrategy> strategies = Arrays.asList(win, macAppleScript, macShellScript, linuxGvfs);
		Optional<MounterStrategy> applicableStrategy = strategies.stream().filter(MounterStrategy::isApplicable).findFirst();
		return applicableStrategy.orElse(fallback);
	}

}
