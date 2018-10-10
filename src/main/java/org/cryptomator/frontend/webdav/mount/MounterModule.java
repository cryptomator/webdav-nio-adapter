package org.cryptomator.frontend.webdav.mount;

import java.util.Set;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;

@Module
public class MounterModule {

	@Provides
	FallbackMounter provideFallbackMounter() {
		return new FallbackMounter();
	}

	@Provides
	@IntoSet
	MounterStrategy provideWindowsMounter() {
		return new WindowsMounter();
	}

	@Provides
	@IntoSet
	MounterStrategy provideMacAppleScriptMounter() {
		return new MacAppleScriptMounter();
	}

	@Provides
	@IntoSet
	MounterStrategy provideMacShellScriptMounter() {
		return new MacShellScriptMounter();
	}

	@Provides
	@IntoSet
	MounterStrategy provideLinuxGioMounter() {
		return new LinuxGioMounter();
	}

	@Provides
	@IntoSet
	MounterStrategy provideLinuxGvfsMounter() {
		return new LinuxGvfsMounter();
	}

	@Provides
	@Singleton
	Mounter provideMounter(FallbackMounter fallback, Set<MounterStrategy> strategies) {
		return strategies.stream().filter(MounterStrategy::isApplicable).findFirst().orElse(fallback);
	}

}
