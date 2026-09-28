# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

The changelog starts with version 2.0.9.
Changes to prior versions can be found on the [Github release page](https://github.com/cryptomator/webdav-nio-adapter/releases).

## [3.0.3] - 2026-09-28

### Fixed
* Drive letter detection in Windows mounter failed on non-English systems ([#150](https://github.com/cryptomator/webdav-nio-adapter/issues/150))

### Changed
* Updated dependencies ([#154](https://github.com/cryptomator/webdav-nio-adapter/pull/154))
    * `org.cryptomator:webdav-nio-adapter-servlet` from 1.2.12 to 1.2.13
    * `org.cryptomator:integrations-api` from 1.8.0 to 1.9.0
    * `org.slf4j:slf4j-api` from 2.0.18 to 2.0.20


## [3.0.2] - 2026-06-04

### Added
* Maven Wrapper ([#147](https://github.com/cryptomator/webdav-nio-adapter/pull/147))

### Changed
* **[BREAKING]** Update build target to JDK 26 ([#146](https://github.com/cryptomator/webdav-nio-adapter/pull/146))
* Updated dependencies ([#144](https://github.com/cryptomator/webdav-nio-adapter/pull/144))
    * `org.cryptomator:webdav-nio-adapter-servlet` from 1.2.11 to 1.2.12
    * `org.cryptomator:integrations-api` from 1.7.0 to 1.8.0
    * `org.slf4j:slf4j-api` from 2.0.17 to 2.0.18
    * `org.slf4j:slf4j-simple` from 2.0.17 to 2.0.18


## [3.0.1] - 2026-02-17

### Changed
* Pin Ci actions ([#129](https://github.com/cryptomator/webdav-nio-adapter/pull/129))
* Updated dependencies
    * `org.cryptomator:integrations-api` from version 1.6.0 to 1.7.0
    * `org.cryptomator:webdav-nio-adapter-servlet` from 1.2.10 to 1.2.11


## [3.0.0] - 2025-09-09

### Changed
* **[BREAKING]** Update build target to JDK 21
* Updated `org.cryptomator:webdav-nio-adapter-servlet` from 1.2.8 to 1.2.10
* Updated `org.cryptomator:integrations-api` from 1.5.1 to 1.6.0
* Updated `org.eclipse.jetty:jetty-server` from 10.0.25 to 10.0.26
* Updated `org.eclipse.jetty:jetty-servlet` from 10.0.25 to 10.0.26


## [2.0.10] - 2025-04-04

### Changed
* Updated org.cryptomator:webdav-nio-adapter-servlet from 1.2.7 to 1.2.8
* Mounting with MacAppleScriptMounter adds credential entry to keychain (again) ([75ef214cd44a3eb84cd4ecc6242456cfa42b35a7](https://github.com/cryptomator/webdav-nio-adapter/commit/75ef214cd44a3eb84cd4ecc6242456cfa42b35a7))


## [2.0.9] - 2025-04-04

### Added
* File /CHANGELOG.md to keep track of changes

## Changed
* Increase timeout for MacAppleScript mounter to 120s (see #107)
* Updated org.eclipse.jetty:jetty-server from 10.0.24 to 10.0.25
* Updated org.eclipse.jetty:jetty-servlet from 10.0.24 to 10.0.25
