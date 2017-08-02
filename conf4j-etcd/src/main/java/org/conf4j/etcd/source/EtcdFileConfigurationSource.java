package org.conf4j.etcd.source;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import mousio.etcd4j.EtcdClient;
import mousio.etcd4j.responses.EtcdException;
import mousio.etcd4j.responses.EtcdKeysResponse;
import org.conf4j.core.source.ConfigurationSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public class EtcdFileConfigurationSource implements ConfigurationSource {

    private static final Logger logger = LoggerFactory.getLogger(EtcdFileConfigurationSource.class);

    private final List<URI> etcdEndpoints;
    private final String configurationPath;
    private final boolean ignoreMissingResource;
    private final AtomicReference<Config> configCache = new AtomicReference<>();

    private EtcdFileConfigurationSource(List<URI> etcdEndpoints, String configurationPath, boolean ignoreMissingFile) {
        this.etcdEndpoints = requireNonNull(etcdEndpoints);
        this.configurationPath = requireNonNull(configurationPath);
        this.ignoreMissingResource = ignoreMissingFile;

        checkArgument(!etcdEndpoints.isEmpty(), "Must supply at least 1 etcd endpoint");
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

    private Config buildConfigIfAbsent(Config currentConfig) {
        if (currentConfig != null) return currentConfig;

        try(EtcdClient etcd = new EtcdClient(etcdEndpoints.toArray(new URI[0]))){
            EtcdKeysResponse etcdKeysResponse = etcd.get(configurationPath).send().get();
            String config = etcdKeysResponse.getNode().getValue();
            if (config != null) {
                return ConfigFactory.parseString(config);
            }
        } catch (EtcdException e) {
            if (!e.ETCDMessage().equals("Key not found")) {
                throw new RuntimeException("Unknown exception while fetching configuration from etcd", e);
            }
        } catch (Exception e) {
            throw new RuntimeException("Unknown exception while fetching configuration from etcd", e);
        }

        logger.debug("Missing configuration file at path: {}, ignore flag set to: {}", configurationPath, ignoreMissingResource);

        if (ignoreMissingResource) {
            return ConfigFactory.empty();
        }

        throw new IllegalStateException("Missing required configuration resource at path: " + configurationPath);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private List<URI> etcdEndpoints;
        private String configurationPath;
        private boolean ignoreMissingPath;

        private Builder() {
            this.etcdEndpoints = new ArrayList<>();
        }

        public Builder addEtcdEndpoint(String endpoint) {
            etcdEndpoints.add(URI.create(endpoint));
            return this;
        }

        public Builder withConfigurationPath(String configurationPath) {
            this.configurationPath = configurationPath;
            return this;
        }

        public Builder ignoreMissingPath() {
            this.ignoreMissingPath = true;
            return this;
        }

        public EtcdFileConfigurationSource build() {
            return new EtcdFileConfigurationSource(etcdEndpoints, configurationPath, ignoreMissingPath);
        }

    }

}
