# jersey-properties

Library to provide property lookup and injection for Jersey projects.

Currently, this project is targeting Java 11+.

## Getting started

### jersey-properties-core

This module contains the majority of the functionality including property
resolving and property deserialisation.

See below for maven coordinates and [here]() for the latest version.

```xml
<dependency>
    <groupId>dev.coldhands</groupId>
    <artifactId>jersey-properties-core</artifactId>
</dependency>
```

### jersey-properties-jakarta

This module depends on `jersey-properties-core` and adds a hk2 injection resolver and jakarta feature for integration
with your jersey application.

See below for maven coordinates and [here]() for the latest version.

```xml
<dependency>
    <groupId>dev.coldhands</groupId>
    <artifactId>jersey-properties-jakarta</artifactId>
</dependency>
```

#### Dependencies

This module's other dependencies are `provided` scope, so you must pull these in yourself though it is compiled against:

```xml
<dependencies>
    <dependency>
        <groupId>jakarta.ws.rs</groupId>
        <artifactId>jakarta.ws.rs-api</artifactId>
        <version>3.x.x</version>
    </dependency>
    <dependency>
        <groupId>org.glassfish.hk2</groupId>
        <artifactId>hk2-api</artifactId>
        <version>3.x.x</version>
    </dependency>
</dependencies>
```

## Documentation

A module with examples has been included [here](examples/src/test/java) 
and javadoc can be found [here]().

## Building this project

The basic requirements to build are:
- Java 11
  - This can be downloaded and managed with [SDKMAN](https://sdkman.io/install)
- Maven
  - This can be downloaded and managed with the included Maven wrapper script

Once SDKMAN has been installed, run `sdk env` to download and initialise your environment with the same
version of Java used in the project.

You should now be able to build the project using `./mvnw clean verify`.