package org.conf4j.core.source;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;

public class ClasspathConfigurationSource implements ConfigurationSource {

    private static final Logger logger = LoggerFactory.getLogger(ClasspathConfigurationSource.class);

    private final String resourcePath;
    private final boolean ignoreMissingResource;
    private final AtomicReference<Config> configCache = new AtomicReference<>();

    private ClasspathConfigurationSource(String resourcePath, boolean ignoreMissingResource) {
        this.resourcePath = requireNonNull(resourcePath);
        this.ignoreMissingResource = ignoreMissingResource;
        configCache.set(buildConfigIfAbsent(null));
    }

    @Override
    public Config getConfig() {
        return configCache.updateAndGet(this::buildConfigIfAbsent);
    }

    private Config buildConfigIfAbsent(Config currentConfig) {
        if (currentConfig != null) return currentConfig;

        if (getClass().getClassLoader().getResource(resourcePath) != null) {
            return ConfigFactory.parseResources(resourcePath);
        }

        logger.debug("Missing configuration resource at path: {}, ignore flag set to: {}", resourcePath, ignoreMissingResource);

        if (ignoreMissingResource) {
            return ConfigFactory.empty();
        }

        throw new IllegalStateException("Missing required configuration resource at path: " + resourcePath);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String resourcePath;
        private boolean ignoreMissingResource;

        private Builder() {}

        public Builder withResourcePath(String resourcePath) {
            this.resourcePath = resourcePath;
            return this;
        }

        public Builder ignoreMissingResource() {
            this.ignoreMissingResource = true;
            return this;
        }

        public ClasspathConfigurationSource build() {
            return new ClasspathConfigurationSource(resourcePath, ignoreMissingResource);
        }

    }

}
