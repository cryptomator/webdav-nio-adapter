# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

The changelog starts with version 2.0.9.
Changes to prior versions can be found on the [Github release page](https://github.com/cryptomator/webdav-nio-adapter/releases).

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
