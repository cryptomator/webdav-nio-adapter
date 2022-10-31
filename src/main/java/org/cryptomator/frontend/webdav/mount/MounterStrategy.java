package org.cryptomator.frontend.webdav.mount;

interface MounterStrategy extends LegacyMounter {

	/**
	 * @return <code>true</code> if the strategy will work on the host this program is running.
	 */
	boolean isApplicable();

}
