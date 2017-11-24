package org.cryptomator.frontend.webdav.mount;

import java.util.HashMap;
import java.util.Map;

public class MountParams extends HashMap<MountParam, String> {

	private MountParams(Map<MountParam, String> params) {
		super(params);
	}

	public static MountParamsBuilder create() {
		return new MountParamsBuilder();
	}

	public static class MountParamsBuilder {

		private final Map<MountParam, String> params = new HashMap<>();

		public MountParamsBuilder with(MountParam key, String value) {
			if (value != null) {
				params.put(key, value);
			}
			return this;
		}

		public MountParamsBuilder withWindowsDriveLetter(String value) {
			if (value == null) {
				return this;
			} else {
				return with(MountParam.WIN_DRIVE_LETTER, value.endsWith(":") ? value : value + ":");
			}
		}

		public MountParamsBuilder withPreferredGvfsScheme(String value) {
			return with(MountParam.PREFERRED_GVFS_SCHEME, value);
		}

		public MountParamsBuilder withWebdavHostname(String value) {
			return with(MountParam.WEBDAV_HOSTNAME, value);
		}

		public MountParams build() {
			return new MountParams(params);
		}

	}

}
