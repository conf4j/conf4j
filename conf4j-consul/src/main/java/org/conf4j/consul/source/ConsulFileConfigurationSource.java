package org.conf4j.consul.source;

import com.orbitz.consul.Consul;
import com.orbitz.consul.KeyValueClient;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.conf4j.core.source.ConfigurationSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;

public class ConsulFileConfigurationSource implements ConfigurationSource, ConsulConfigurationSource {

    private static final Logger logger = LoggerFactory.getLogger(ConsulFileConfigurationSource.class);

    private final KeyValueClient kvClient;
    private final String configurationFilePath;
    private final boolean ignoreMissingResource;
    private final Duration readTimeout;
    private final AtomicReference<Config> configCache = new AtomicReference<>();

    private ConsulFileConfigurationSource(KeyValueClient kvClient, String configurationFilePath,
                                          boolean ignoreMissingFile, Duration readTimeout) {
        this.kvClient = requireNonNull(kvClient);
        this.configurationFilePath = requireNonNull(configurationFilePath);
        this.ignoreMissingResource = ignoreMissingFile;
        this.readTimeout = requireNonNull(readTimeout);
        configCache.set(buildConfigIfAbsent(null));
    }

    @Override
    public Config getConfig() {
        return configCache.updateAndGet(this::buildConfigIfAbsent);
    }

    @Override
    public void reload() {
        configCache.set(this.buildConfigIfAbsent(null));
    }

    @Override
    public KeyValueClient getKeyValueClient() {
        return kvClient;
    }

    @Override
    public String getPathToWatch() {
        return configurationFilePath;
    }

    @Override
    public Duration getReadTimeout() {
        return readTimeout;
    }

    private Config buildConfigIfAbsent(Config currentConfig) {
        if (currentConfig != null) return currentConfig;

        Optional<String> configurationFile = kvClient.getValueAsString(configurationFilePath).toJavaUtil();
        if (configurationFile.isPresent()) {
            return ConfigFactory.parseString(configurationFile.get());
        }

        logger.debug("Missing configuration file at path: {}, ignore flag set to: {}", configurationFilePath, ignoreMissingResource);

        if (ignoreMissingResource) {
            return ConfigFactory.empty();
        }

        throw new IllegalStateException("Missing required configuration resource at path: " + configurationFilePath);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Duration readTimeout = Duration.ofSeconds(5);
        private Consul.Builder consulBuilder;
        private String configurationFilePath;
        private boolean ignoreMissingResource;

        private Builder() {
            this.consulBuilder = Consul.builder()
                    .withReadTimeoutMillis(readTimeout.toMillis());
        }

        public Builder withConsulUrl(String consulUrl) {
            consulBuilder.withUrl(consulUrl);
            return this;
        }

        public Builder withAclToken(String aclToken) {
            consulBuilder.withAclToken(aclToken);
            return this;
        }

        public Builder withBasicAuth(String username, String password) {
            consulBuilder.withBasicAuth(username, password);
            return this;
        }

        public Builder withConnectTimeoutMillis(long timeoutMillis) {
            consulBuilder.withConnectTimeoutMillis(timeoutMillis);
            return this;
        }

        public Builder withReadTimeoutMillis(long timeoutMillis) {
            consulBuilder.withReadTimeoutMillis(timeoutMillis);
            readTimeout = Duration.ofMillis(timeoutMillis);
            return this;
        }

        public Builder withConfigurationFilePath(String configurationFilePath) {
            this.configurationFilePath = configurationFilePath;
            return this;
        }

        public Builder ignoreMissingResource() {
            this.ignoreMissingResource = true;
            return this;
        }

        public ConsulFileConfigurationSource build() {
            Consul consul = consulBuilder.build();
            KeyValueClient kvClient = consul.keyValueClient();
            return new ConsulFileConfigurationSource(kvClient, configurationFilePath, ignoreMissingResource, readTimeout);
        }

    }

}
