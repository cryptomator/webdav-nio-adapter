package org.cryptomator.frontend.webdav;

public interface ContextPathRegistry {

	boolean add(String contextPath);
	boolean remove(String contextPath);

}
