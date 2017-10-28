# Configuration 4 Java
[![Build Status](https://travis-ci.org/conf4j/conf4j.svg?branch=master)](https://travis-ci.org/conf4j/conf4j)
[![Coverage Status](https://coveralls.io/repos/conf4j/conf4j/badge.svg?branch=master)](https://coveralls.io/r/conf4j/conf4j?branch=master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.conf4j/conf4j-core/badge.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.conf4j%22)

## Installation

### Gradle

```groovy
compile 'org.conf4j:conf4j-core:2017.10.1'
```

### Maven

```xml
<dependency>
  <groupId>org.conf4j</groupId>
  <artifactId>conf4j-core</artifactId>
  <version>2017.10.1</version>
</dependency>
```

## Usage

### Getting Started

1. Add `conf4j` dependency to your project
2. Build your configuration provider using `ConfigurationProviderBuilder`
3. Get an instance of your config bean from the configuration provider

```java
public class Main {
  public static void main(String[] args) throws Exception {
    ConsulFileConfigurationSource prodConfigSource = ConsulFileConfigurationSource.builder()
        .withConfigurationFilePath("prod/test-service.conf")
        .withConsulUrl("localhost:8500")
        .reloadOnChange()
        .build();

    ClasspathConfigurationSource fallbackConfigSource = ClasspathConfigurationSource.builder()
        .withResourcePath("fallback.conf")
        .build();

    ConfigurationProvider<TestConfigBean> provider = new ConfigurationProviderBuilder<>(TestConfigBean.class)
        .withConfigurationSource(prodConfigSource)
        .addFallback(fallbackConfigSource)
        .build();

    provider.registerChangeListener((oldConfig, newConfig) -> myListener(newConfig));
    TestConfigBean config = provider.get();
  }
}
```

### Contribution
 - Fork
 - Code
 - ```./mvnw test```
 - Issue a PR :)
