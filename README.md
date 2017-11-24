[![Build Status](https://travis-ci.org/cryptomator/webdav-nio-adapter.svg?branch=develop)](https://travis-ci.org/cryptomator/webdav-nio-adapter)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/cb25035c651d4cbf92d21e00137f47d7)](https://www.codacy.com/app/cryptomator/webdav-nio-adapter?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=cryptomator/webdav-nio-adapter&amp;utm_campaign=Badge_Grade)
[![Codacy Badge](https://api.codacy.com/project/badge/Coverage/cb25035c651d4cbf92d21e00137f47d7)](https://www.codacy.com/app/cryptomator/webdav-nio-adapter?utm_source=github.com&utm_medium=referral&utm_content=cryptomator/webdav-nio-adapter&utm_campaign=Badge_Coverage)
[![Maven Central](https://img.shields.io/maven-central/v/org.cryptomator/webdav-nio-adapter.svg?maxAge=86400)](https://repo1.maven.org/maven2/org/cryptomator/webdav-nio-adapter/)
[![Javadocs](http://www.javadoc.io/badge/org.cryptomator/webdav-nio-adapter.svg)](http://www.javadoc.io/doc/org.cryptomator/webdav-nio-adapter)

# webdav-nio-adapter
Serves directory contents specified by a `java.nio.file.Path` via a WebDAV servlet.

Uses Jackrabbit and an embedded Jetty to server the servlet.

## Maven integration

```xml
<dependencies>
  <dependency>
    <groupId>org.cryptomator</groupId>
    <artifactId>webdav-nio-adapter</artifactId>
    <version>1.0.0</version>
  </dependency>
</dependencies>
```

## License

This project is dual-licensed under the AGPLv3 for FOSS projects as well as a commercial license for independent software vendors and resellers. If you want to use this library in applications, that are *not* licensed under the AGPL, feel free to contact our [support team](https://cryptomator.org/help/).