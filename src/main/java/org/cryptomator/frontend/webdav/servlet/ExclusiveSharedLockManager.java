/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE.txt.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.frontend.webdav.servlet;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.inject.Inject;

import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.lock.ActiveLock;
import org.apache.jackrabbit.webdav.lock.LockInfo;
import org.apache.jackrabbit.webdav.lock.LockManager;
import org.apache.jackrabbit.webdav.lock.Scope;
import org.apache.jackrabbit.webdav.lock.Type;

@PerServlet
class ExclusiveSharedLockManager implements LockManager {

	private final ConcurrentMap<DavLocatorImpl, Map<String, ActiveLock>> lockedResources = new ConcurrentHashMap<>();

	@Inject
	public ExclusiveSharedLockManager() {
	}

	@Override
	public ActiveLock createLock(LockInfo lockInfo, DavResource resource) throws DavException {
		Objects.requireNonNull(lockInfo);
		Objects.requireNonNull(resource);
		if (resource instanceof DavNode) {
			return createLockInternal(lockInfo, (DavNode) resource);
		} else {
			throw new IllegalArgumentException("Unsupported resource type " + resource.getClass());
		}
	}

	private synchronized ActiveLock createLockInternal(LockInfo lockInfo, DavNode resource) throws DavException {
		DavLocatorImpl locator = resource.getLocator();
		removedExpiredLocksInLocatorHierarchy(locator);

		// look for existing locks on this resource or its ancestors:
		ActiveLock existingExclusiveLock = getLock(lockInfo.getType(), Scope.EXCLUSIVE, resource);
		ActiveLock existingSharedLock = getLock(lockInfo.getType(), Scope.SHARED, resource);
		boolean hasExclusiveLock = existingExclusiveLock != null;
		boolean hasSharedLock = existingSharedLock != null;
		boolean isLocked = hasExclusiveLock || hasSharedLock;
		if ((Scope.EXCLUSIVE.equals(lockInfo.getScope()) && isLocked) || (Scope.SHARED.equals(lockInfo.getScope()) && hasExclusiveLock)) {
			throw new DavException(DavServletResponse.SC_LOCKED, "Resource (or parent resource) already locked.");
		}

		// look for locked children:
		for (Entry<DavLocatorImpl, Map<String, ActiveLock>> potentialChild : lockedResources.entrySet()) {
			final DavLocatorImpl childLocator = potentialChild.getKey();
			final Collection<ActiveLock> childLocks = potentialChild.getValue().values();
			if (isChild(locator, childLocator) && isAffectedByChildLocks(lockInfo, locator, childLocks, childLocator)) {
				throw new DavException(DavServletResponse.SC_CONFLICT, "Subresource already locked. " + childLocator);
			}
		}

		String token = DavConstants.OPAQUE_LOCK_TOKEN_PREFIX + UUID.randomUUID();
		Map<String, ActiveLock> lockMap = Objects.requireNonNull(lockedResources.computeIfAbsent(locator, loc -> new HashMap<>()));
		return lockMap.computeIfAbsent(token, t -> new ExclusiveSharedLock(t, lockInfo));
	}

	private void removedExpiredLocksInLocatorHierarchy(DavLocatorImpl locator) {
		Objects.requireNonNull(locator);
		lockedResources.getOrDefault(locator, Collections.emptyMap()).values().removeIf(ActiveLock::isExpired);
		if (!locator.isRootLocation()) {
			this.removedExpiredLocksInLocatorHierarchy(locator.resolveParent());
		}
	}

	private boolean isChild(DavResourceLocator parent, DavResourceLocator child) {
		return child.getResourcePath().startsWith(parent.getResourcePath());
	}

	private boolean isAffectedByChildLocks(LockInfo parentLockInfo, DavLocatorImpl parentLocator, Collection<ActiveLock> childLocks, DavLocatorImpl childLocator) {
		for (ActiveLock lock : childLocks) {
			if (Scope.SHARED.equals(lock.getScope()) && Scope.SHARED.equals(parentLockInfo.getScope())) {
				continue;
			} else if (parentLockInfo.isDeep() || childLocator.resolveParent().equals(parentLocator)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public ActiveLock refreshLock(LockInfo lockInfo, String lockToken, DavResource resource) throws DavException {
		ActiveLock lock = getLock(lockInfo.getType(), lockInfo.getScope(), resource);
		if (lock == null) {
			throw new DavException(DavServletResponse.SC_PRECONDITION_FAILED);
		} else if (!lock.isLockedByToken(lockToken)) {
			throw new DavException(DavServletResponse.SC_LOCKED);
		}
		lock.setTimeout(lockInfo.getTimeout());
		return lock;
	}

	@Override
	public synchronized void releaseLock(String lockToken, DavResource resource) throws DavException {
		if (resource instanceof DavNode) {
			try {
				releaseLockInternal(lockToken, (DavNode) resource);
			} catch (UncheckedDavException e) {
				throw e.toDavException();
			}
		} else {
			throw new IllegalArgumentException("Unsupported resource type " + resource.getClass());
		}
	}

	private synchronized void releaseLockInternal(String lockToken, DavNode resource) throws UncheckedDavException {
		lockedResources.compute(resource.getLocator(), (loc, locks) -> {
			if (locks == null || locks.isEmpty()) {
				// no lock exists, nothing needs to change.
				return null;
			} else if (!locks.containsKey(lockToken)) {
				throw new UncheckedDavException(DavServletResponse.SC_LOCKED, "Resource locked with different token.");
			} else {
				locks.remove(lockToken);
				return locks.isEmpty() ? null : locks;
			}
		});
	}

	@Override
	public ActiveLock getLock(Type type, Scope scope, DavResource resource) {
		if (resource instanceof DavNode) {
			DavNode node = (DavNode) resource;
			return getLockInternal(type, scope, node.getLocator(), 0);
		} else {
			throw new IllegalArgumentException("Unsupported resource type " + resource.getClass());
		}
	}

	private ActiveLock getLockInternal(Type type, Scope scope, DavLocatorImpl locator, int depth) {
		// try to find a lock directly on this resource:
		if (lockedResources.containsKey(locator)) {
			for (ActiveLock lock : lockedResources.get(locator).values()) {
				if (type.equals(lock.getType()) && scope.equals(lock.getScope()) && (depth == 0 || lock.isDeep())) {
					return lock;
				}
			}
		}
		// or otherwise look for parent locks (if there is a parent):
		if (locator.isRootLocation()) {
			return null;
		} else {
			return getLockInternal(type, scope, locator.resolveParent(), depth + 1);
		}
	}

	@Override
	public boolean hasLock(String lockToken, DavResource resource) {
		if (resource instanceof DavNode) {
			DavNode node = (DavNode) resource;
			return lockedResources.getOrDefault(node.getLocator(), Collections.emptyMap()).containsKey(lockToken);
		} else {
			throw new IllegalArgumentException("Unsupported resource type " + resource.getClass());
		}
	}

}
