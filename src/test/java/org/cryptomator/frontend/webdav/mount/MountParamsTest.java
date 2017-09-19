package org.cryptomator.frontend.webdav.mount;

import org.junit.Assert;
import org.junit.Test;

public class MountParamsTest {

	@Test
	public void testNullSaveWithWindowsDriveLetter() {
		MountParams params = MountParams.create().withWindowsDriveLetter(null).build();
		Assert.assertNull(params.get(MountParam.WIN_DRIVE_LETTER));
	}

	@Test
	public void testNullSaveWithPreferredGvfsScheme() {
		MountParams params = MountParams.create().withPreferredGvfsScheme(null).build();
		Assert.assertNull(params.get(MountParam.PREFERRED_GVFS_SCHEME));
	}

	@Test
	public void testNullSaveWithWebdavHostname() {
		MountParams params = MountParams.create().withWebdavHostname(null).build();
		Assert.assertNull(params.get(MountParam.WEBDAV_HOSTNAME));
	}

}
