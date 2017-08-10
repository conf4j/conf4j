package org.conf4j.consul.source;

import com.orbitz.consul.Consul;
import com.orbitz.consul.KeyValueClient;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.conf4j.consul.source.reload.ConsulWatchReloadStrategy;
import org.conf4j.core.source.WatchableConfigurationSource;
import org.conf4j.core.source.reload.ReloadStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;

public class ConsulFileConfigurationSource implements WatchableConfigurationSource, ConsulConfigurationSource {

    private static final Logger logger = LoggerFactory.getLogger(ConsulFileConfigurationSource.class);

    private final KeyValueClient kvClient;
    private final String configurationFilePath;
    private final boolean ignoreMissingResource;
    private final Duration watchTimeout;
    private final AtomicReference<Config> configCache = new AtomicReference<>();
    private final ConsulWatchReloadStrategy reloadStrategy;

    private ConsulFileConfigurationSource(KeyValueClient kvClient, String configurationFilePath,
                                          boolean ignoreMissingFile, boolean reloadOnChange,
                                          Duration watchTimeout) {
        this.kvClient = requireNonNull(kvClient);
        this.configurationFilePath = requireNonNull(configurationFilePath);
        this.ignoreMissingResource = ignoreMissingFile;
        this.watchTimeout = requireNonNull(watchTimeout);
        configCache.set(buildConfigIfAbsent(null));

        if (reloadOnChange) {
            this.reloadStrategy = ConsulWatchReloadStrategy.builder()
                    .withConsulConfigurationSource(this)
                    .build();
        } else {
            this.reloadStrategy = null;
        }
    }

    @Override
    public Config getConfig() {
        return configCache.updateAndGet(this::buildConfigIfAbsent);
    }

    @Override
    public boolean shouldWatchForChange() {
        return reloadStrategy != null;
    }

    @Override
    public ReloadStrategy getReloadStrategy() {
        return reloadStrategy;
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
    public Duration getWatchTimeout() {
        return watchTimeout;
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

        private Duration watchTimeout = Duration.ofSeconds(10);
        private Consul.Builder consulBuilder;
        private String configurationFilePath;
        private boolean ignoreMissingResource;
        private boolean reloadOnChange;

        private Builder() {
            this.consulBuilder = Consul.builder()
                    .withReadTimeoutMillis(Duration.ofSeconds(30).toMillis());
        }

        public Builder withConsulUrl(String consulUrl) {
            if (!consulUrl.startsWith("http")) {
                consulUrl = "http://" + consulUrl;
            }

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

        public Builder withConnectTimeout(Duration connectTimeout) {
            consulBuilder.withConnectTimeoutMillis(connectTimeout.toMillis());
            return this;
        }

        public Builder withReadTimeout(Duration readTimeout) {
            consulBuilder.withReadTimeoutMillis(readTimeout.toMillis());
            return this;
        }

        public Builder withWatchTimeout(Duration watchTimeout) {
            this.watchTimeout = watchTimeout;
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

        public Builder reloadOnChange() {
            this.reloadOnChange = true;
            return this;
        }

        public ConsulFileConfigurationSource build() {
            Consul consul = consulBuilder.build();
            KeyValueClient kvClient = consul.keyValueClient();
            return new ConsulFileConfigurationSource(kvClient, configurationFilePath, ignoreMissingResource,
                    reloadOnChange, watchTimeout);
        }

    }

}
