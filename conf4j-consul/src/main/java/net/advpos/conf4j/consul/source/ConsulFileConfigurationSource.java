package net.advpos.conf4j.consul.source;

import com.orbitz.consul.Consul;
import com.orbitz.consul.KeyValueClient;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import net.advpos.conf4j.core.source.ConfigurationSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;

public class ConsulFileConfigurationSource implements ConfigurationSource {

    private static final Logger logger = LoggerFactory.getLogger(ConsulFileConfigurationSource.class);

    private final Consul consul;
    private final String configurationFilePath;
    private final boolean ignoreMissingResource;
    private final AtomicReference<Config> configCache = new AtomicReference<>();

    private ConsulFileConfigurationSource(Consul consul, String configurationFilePath, boolean ignoreMissingFile) {
        this.consul = requireNonNull(consul);
        this.configurationFilePath = requireNonNull(configurationFilePath);
        this.ignoreMissingResource = ignoreMissingFile;
        configCache.set(buildConfigIfAbsent(null));
    }

    @Override
    public Config getConfig() {
        return configCache.updateAndGet(this::buildConfigIfAbsent);
    }

    private Config buildConfigIfAbsent(Config currentConfig) {
        if (currentConfig != null) return currentConfig;

        KeyValueClient kvClient = consul.keyValueClient();
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

        private Consul.Builder consulBuilder;
        private String configurationFilePath;
        private boolean ignoreMissingResource;

        private Builder() {
            this.consulBuilder = Consul.builder();
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
            return new ConsulFileConfigurationSource(consul, configurationFilePath, ignoreMissingResource);
        }

    }

}
