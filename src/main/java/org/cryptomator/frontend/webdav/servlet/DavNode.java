/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE.txt.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.frontend.webdav.servlet;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.lock.ActiveLock;
import org.apache.jackrabbit.webdav.lock.LockInfo;
import org.apache.jackrabbit.webdav.lock.LockManager;
import org.apache.jackrabbit.webdav.lock.Scope;
import org.apache.jackrabbit.webdav.lock.Type;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;
import org.apache.jackrabbit.webdav.property.PropEntry;

abstract class DavNode implements DavResource {

	private static final String DAV_COMPLIANCE_CLASSES = "1, 2";
	private static final String[] DAV_CREATIONDATE_PROPNAMES = {DavConstants.PROPERTY_CREATIONDATE, "Win32CreationTime"};
	private static final String[] DAV_MODIFIEDDATE_PROPNAMES = {DavConstants.PROPERTY_GETLASTMODIFIED, "Win32LastModifiedTime"};

	protected final DavResourceFactoryImpl factory;
	protected final LockManager lockManager;
	protected final DavLocatorImpl locator;
	protected final Path path;
	protected final Optional<BasicFileAttributes> attr;
	protected final DavSession session;
	protected final DavPropertySet properties;

	public DavNode(DavResourceFactoryImpl factory, LockManager lockManager, DavLocatorImpl locator, Path path, Optional<BasicFileAttributes> attr, DavSession session) {
		this.factory = factory;
		this.lockManager = lockManager;
		this.locator = locator;
		this.path = path;
		this.attr = attr;
		this.session = session;
		this.properties = new DavPropertySet();
	}

	@Override
	public String getComplianceClass() {
		return DAV_COMPLIANCE_CLASSES;
	}

	@Override
	public String getSupportedMethods() {
		return METHODS;
	}

	@Override
	public boolean exists() {
		return attr.isPresent();
	}

	@Override
	public String getDisplayName() {
		String[] pathElements = StringUtils.split(getResourcePath(), '/');
		return pathElements[pathElements.length - 1];
	}

	@Override
	public DavLocatorImpl getLocator() {
		return locator;
	}

	@Override
	public String getResourcePath() {
		return locator.getResourcePath();
	}

	@Override
	public String getHref() {
		return locator.getHref(isCollection());
	}

	@Override
	public long getModificationTime() {
		return attr.map(BasicFileAttributes::lastModifiedTime).map(FileTime::toInstant).map(Instant::toEpochMilli).orElse(-1l);
	}

	protected void setModificationTime(Instant instant) throws DavException {
		BasicFileAttributeView attrView = Files.getFileAttributeView(path, BasicFileAttributeView.class);
		if (attrView != null) {
			try {
				attrView.setTimes(FileTime.from(instant), null, null);
			} catch (IOException e) {
				throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, e);
			}
		}
	}

	protected void setCreationTime(Instant instant) throws DavException {
		BasicFileAttributeView attrView = Files.getFileAttributeView(path, BasicFileAttributeView.class);
		if (attrView != null) {
			try {
				attrView.setTimes(null, null, FileTime.from(instant));
			} catch (IOException e) {
				throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, e);
			}
		}
	}

	@Override
	public DavPropertyName[] getPropertyNames() {
		return getProperties().getPropertyNames();
	}

	@Override
	public DavProperty<?> getProperty(DavPropertyName name) {
		final String namespacelessPropertyName = name.getName();
		if (Arrays.asList(DAV_CREATIONDATE_PROPNAMES).contains(namespacelessPropertyName)) {
			return creationDateProperty(name).orElse(null);
		} else if (Arrays.asList(DAV_MODIFIEDDATE_PROPNAMES).contains(namespacelessPropertyName)) {
			return lastModifiedDateProperty(name).orElse(null);
		} else {
			return properties.get(name);
		}
	}

	/**
	 * Returns a current snapshot of all available properties.
	 */
	@Override
	public DavPropertySet getProperties() {
		creationDateProperty(DavPropertyName.CREATIONDATE).ifPresent(properties::add);
		lastModifiedDateProperty(DavPropertyName.GETLASTMODIFIED).ifPresent(properties::add);
		return properties;
	}

	private Optional<DavProperty<?>> lastModifiedDateProperty(DavPropertyName name) {
		return attr.map(BasicFileAttributes::lastModifiedTime) //
				.map(FileTime::toInstant) //
				.map(creationTime -> OffsetDateTime.ofInstant(creationTime, ZoneOffset.UTC)) //
				.map(creationDate -> new DefaultDavProperty<>(name, DateTimeFormatter.RFC_1123_DATE_TIME.format(creationDate)));
	}

	private Optional<DavProperty<?>> creationDateProperty(DavPropertyName name) {
		return attr.map(BasicFileAttributes::creationTime) //
				.map(FileTime::toInstant) //
				.map(creationTime -> OffsetDateTime.ofInstant(creationTime, ZoneOffset.UTC)) //
				.map(creationDate -> new DefaultDavProperty<>(name, DateTimeFormatter.RFC_1123_DATE_TIME.format(creationDate)));
	}

	@Override
	public void setProperty(DavProperty<?> property) throws DavException {
		final String namespacelessPropertyName = property.getName().getName();
		if (Arrays.asList(DAV_CREATIONDATE_PROPNAMES).contains(namespacelessPropertyName) && property.getValue() instanceof String) {
			String createDateStr = (String) property.getValue();
			OffsetDateTime creationDate = OffsetDateTime.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(createDateStr));
			this.setCreationTime(creationDate.toInstant());
		} else if (Arrays.asList(DAV_MODIFIEDDATE_PROPNAMES).contains(namespacelessPropertyName) && property.getValue() instanceof String) {
			String lastModifiedDateStr = (String) property.getValue();
			OffsetDateTime lastModifiedDate = OffsetDateTime.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(lastModifiedDateStr));
			this.setModificationTime(lastModifiedDate.toInstant());
		}
		properties.add(property);
	}

	@Override
	public void removeProperty(DavPropertyName propertyName) throws DavException {
		getProperties().remove(propertyName);
	}

	@Override
	public MultiStatusResponse alterProperties(List<? extends PropEntry> changeList) throws DavException {
		final DavPropertyNameSet names = new DavPropertyNameSet();
		for (final PropEntry entry : changeList) {
			if (entry instanceof DavProperty) {
				final DavProperty<?> prop = (DavProperty<?>) entry;
				this.setProperty(prop);
				names.add(prop.getName());
			} else if (entry instanceof DavPropertyName) {
				final DavPropertyName name = (DavPropertyName) entry;
				this.removeProperty(name);
				names.add(name);
			}
		}
		return new MultiStatusResponse(this, names);
	}

	@Override
	public DavFolder getCollection() {
		DavLocatorImpl parentLocator = locator.resolveParent();
		if (parentLocator == null) {
			return null;
		} else {
			Path parentPath = path.getParent();
			BasicFileAttributes parentAttr;
			try {
				parentAttr = Files.readAttributes(parentPath, BasicFileAttributes.class);
			} catch (NoSuchFileException e) {
				parentAttr = null;
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			return factory.createFolder(parentLocator, parentPath, Optional.of(parentAttr), session);
		}
	}

	@Override
	public boolean isLockable(Type type, Scope scope) {
		return Type.WRITE.equals(type) && Scope.EXCLUSIVE.equals(scope) || Scope.SHARED.equals(scope);
	}

	@Override
	public boolean hasLock(Type type, Scope scope) {
		return getLock(type, scope) != null;
	}

	@Override
	public ActiveLock getLock(Type type, Scope scope) {
		return lockManager.getLock(type, scope, this);
	}

	@Override
	public ActiveLock[] getLocks() {
		final ActiveLock exclusiveWriteLock = getLock(Type.WRITE, Scope.EXCLUSIVE);
		final ActiveLock sharedWriteLock = getLock(Type.WRITE, Scope.SHARED);
		return Stream.of(exclusiveWriteLock, sharedWriteLock).filter(Objects::nonNull).toArray(ActiveLock[]::new);
	}

	@Override
	public ActiveLock lock(LockInfo reqLockInfo) throws DavException {
		return lockManager.createLock(reqLockInfo, this);
	}

	@Override
	public ActiveLock refreshLock(LockInfo reqLockInfo, String lockToken) throws DavException {
		return lockManager.refreshLock(reqLockInfo, lockToken, this);
	}

	@Override
	public void unlock(String lockToken) throws DavException {
		lockManager.releaseLock(lockToken, this);
	}

	@Override
	public void addLockManager(LockManager lockmgr) {
		throw new UnsupportedOperationException("Locks are managed");
	}

	@Override
	public DavResourceFactoryImpl getFactory() {
		return factory;
	}

	@Override
	public DavSession getSession() {
		return session;
	}

}
