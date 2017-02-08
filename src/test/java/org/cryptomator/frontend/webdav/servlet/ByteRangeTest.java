/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.frontend.webdav.servlet;

import org.cryptomator.frontend.webdav.servlet.ByteRange.MalformedByteRangeException;
import org.cryptomator.frontend.webdav.servlet.ByteRange.UnsupportedRangeException;
import org.junit.Assert;
import org.junit.Test;

public class ByteRangeTest {

	@Test(expected = UnsupportedRangeException.class)
	public void testConstructionWithUnsupportedRangeType() throws UnsupportedRangeException, MalformedByteRangeException {
		new ByteRange("cats=2-3");
	}

	@Test(expected = UnsupportedRangeException.class)
	public void testConstructionWithMultipleRanges() throws UnsupportedRangeException, MalformedByteRangeException {
		new ByteRange("bytes=2-3,7-8");
	}

	@Test(expected = MalformedByteRangeException.class)
	public void testConstructionWithMalformedSingleRange1() throws UnsupportedRangeException, MalformedByteRangeException {
		new ByteRange("bytes=2-3-4");
	}

	@Test(expected = MalformedByteRangeException.class)
	public void testConstructionWithMalformedSingleRange2() throws UnsupportedRangeException, MalformedByteRangeException {
		new ByteRange("bytes=3-2");
	}

	@Test(expected = MalformedByteRangeException.class)
	public void testConstructionWithMalformedSingleRange3() throws UnsupportedRangeException, MalformedByteRangeException {
		new ByteRange("bytes=-");
	}

	@Test(expected = MalformedByteRangeException.class)
	public void testConstructionWithMalformedSingleRange4() throws UnsupportedRangeException, MalformedByteRangeException {
		new ByteRange("bytes=");
	}

	@Test(expected = MalformedByteRangeException.class)
	public void testConstructionWithMalformedSingleRange5() throws UnsupportedRangeException, MalformedByteRangeException {
		new ByteRange("bytes=2-z");
	}

	@Test
	public void testConstructionWithSingleClosedRange() throws UnsupportedRangeException, MalformedByteRangeException {
		ByteRange range = new ByteRange("bytes=2-3");
		Assert.assertEquals(2, range.getEffectiveFirstByte(1000));
		Assert.assertEquals(3, range.getEffectiveLastByte(1000));
	}

	@Test
	public void testConstructionWithSingleOpenRange1() throws UnsupportedRangeException, MalformedByteRangeException {
		ByteRange range = new ByteRange("bytes=2-");
		Assert.assertEquals(2, range.getEffectiveFirstByte(1000));
		Assert.assertEquals(999, range.getEffectiveLastByte(1000));
	}

	@Test
	public void testConstructionWithSingleOpenRange2() throws UnsupportedRangeException, MalformedByteRangeException {
		ByteRange range = new ByteRange("bytes=-2");
		Assert.assertEquals(998, range.getEffectiveFirstByte(1000));
		Assert.assertEquals(999, range.getEffectiveLastByte(1000));
	}

}
