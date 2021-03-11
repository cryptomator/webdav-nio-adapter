package org.cryptomator.frontend.webdav.mount;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MountParamsTest {

	@Test
	public void testNullSaveWithWindowsDriveLetter() {
		MountParams params = MountParams.create().withWindowsDriveLetter(null).build();
		Assertions.assertNull(params.get(MountParam.WIN_DRIVE_LETTER));
	}

	@Test
	public void testNullSaveWithWindowsDriveLetterAfterFirstMountWithNonNullMount() {
		MountParams paramsFirst = MountParams.create().withWindowsDriveLetter("A:").build();
		MountParams paramsSecond = MountParams.create().withWindowsDriveLetter(null).build();

		Assertions.assertEquals("A:", paramsFirst.get(MountParam.WIN_DRIVE_LETTER));
		Assertions.assertNull(paramsSecond.get(MountParam.WIN_DRIVE_LETTER));
		
	}

	@Test
	public void testNullAfterFirstMountWithNonNullMount() {
		MountParams paramsFirst = MountParams.create().withWindowsDriveLetter("A:").build();
		MountParams paramsSecond = MountParams.create().build();

		Assertions.assertEquals("A:", paramsFirst.get(MountParam.WIN_DRIVE_LETTER));
		Assertions.assertNull(paramsSecond.get(MountParam.WIN_DRIVE_LETTER));

	}
	
	@Test
	public void testNullSaveWithPreferredGvfsScheme() {
		MountParams params = MountParams.create().withPreferredGvfsScheme(null).build();
		Assertions.assertNull(params.get(MountParam.PREFERRED_GVFS_SCHEME));
	}

	@Test
	public void testNullSaveWithWebdavHostname() {
		MountParams params = MountParams.create().withWebdavHostname(null).build();
		Assertions.assertNull(params.get(MountParam.WEBDAV_HOSTNAME));
	}

}
