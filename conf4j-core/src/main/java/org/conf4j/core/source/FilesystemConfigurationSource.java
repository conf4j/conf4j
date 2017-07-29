package org.conf4j.core.source;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;

public class FilesystemConfigurationSource implements ConfigurationSource {

    private static final Logger logger = LoggerFactory.getLogger(FilesystemConfigurationSource.class);

    private final String filePath;
    private final boolean ignoreMissingFile;
    private final AtomicReference<Config> configCache = new AtomicReference<>();

    private FilesystemConfigurationSource(String filePath, boolean ignoreMissingFile) {
        this.filePath = requireNonNull(filePath);
        this.ignoreMissingFile = ignoreMissingFile;
        configCache.set(buildConfigIfAbsent(null));
    }

    @Override
    public Config getConfig() {
        return configCache.updateAndGet(this::buildConfigIfAbsent);
    }

    private Config buildConfigIfAbsent(Config currentConfig) {
        if (currentConfig != null) return currentConfig;

        File configFile = new File(filePath);
        if (configFile.exists()) {
            return ConfigFactory.parseFile(configFile);
        }

        logger.debug("Missing configuration file at path: {}, ignore flag set to: {}", filePath, ignoreMissingFile);

        if (ignoreMissingFile) {
            return ConfigFactory.empty();
        }

        throw new IllegalStateException("Missing required configuration file at path: " + filePath);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String filePath;
        private boolean ignoreMissingFile;

        private Builder() {}

        public Builder withFilePath(String filePath) {
            this.filePath = filePath;
            return this;
        }

        public Builder ignoreMissingFile() {
            this.ignoreMissingFile = true;
            return this;
        }

        public FilesystemConfigurationSource build() {
            return new FilesystemConfigurationSource(filePath, ignoreMissingFile);
        }

    }

}
