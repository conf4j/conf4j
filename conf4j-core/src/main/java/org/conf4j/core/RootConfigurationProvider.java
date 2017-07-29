package org.conf4j.core;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.typesafe.config.Config;
import org.conf4j.core.source.ConfigurationSource;
import org.conf4j.core.source.MergeConfigurationSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;

public class RootConfigurationProvider<T> extends ConfigurationProvider<T> {

    private static final Logger logger = LoggerFactory.getLogger(RootConfigurationProvider.class);

    private final ObjectMapper mapper;
    private final Class<? extends T> configurationClass;
    private final ConfigurationSource configurationSource;
    private final AtomicReference<ConfigHolder<T>> configurationCache = new AtomicReference<>();

    private RootConfigurationProvider(Class<? extends T> configurationClass, ConfigurationSource configurationSource) {
        this.configurationClass = requireNonNull(configurationClass);
        this.configurationSource = requireNonNull(configurationSource);
        this.mapper = createObjectMapper();
    }

    public T get() {
        return configurationCache.updateAndGet(this::buildConfigObjectIfNeeded).getConfiguration();
    }

    public Config getConfig() {
        return configurationCache.updateAndGet(this::buildConfigObjectIfNeeded).getTypeSafeConfig();
    }

    private ConfigHolder<T> buildConfigObjectIfNeeded(ConfigHolder<T> currentConfig) {
        if (currentConfig != null) return currentConfig;

        Config config = configurationSource.getConfig();
        config.resolve();

        try {
            Map<String, Object> configMap = config.root().unwrapped();
            T configuration = mapper.convertValue(configMap, configurationClass);
            return new ConfigHolder<>(config, configuration);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create configuration object", e);
        }
    }

    public static <T> Builder<T> builder(Class<? extends T> configurationClass) {
        return new Builder<>(configurationClass);
    }

    private void reload() {
        ConfigHolder<T> oldConfigHolder = configurationCache.get();
        ConfigHolder<T> newConfigHolder = buildConfigObjectIfNeeded(null);
        configurationCache.set(newConfigHolder);

        if (oldConfigHolder.getTypeSafeConfig().equals(newConfigHolder.getTypeSafeConfig())) {
            logger.debug("Skipping notifying listeners about config reload, configurations are identical");
            return;
        }

        T oldConfig = oldConfigHolder.getConfiguration();
        T newConfig = newConfigHolder.getConfiguration();
        notifyListenersOnConfigChangeIfNeeded(oldConfig, newConfig);
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        return mapper;
    }

    public static class Builder<T> {

        private Class<? extends T> configurationClass;
        private ConfigurationSource configurationSource;

        private Builder(Class<? extends T> configurationClass) {
            this.configurationClass = configurationClass;
        }

        public Builder<T> withConfigurationSource(ConfigurationSource configurationSource) {
            this.configurationSource = configurationSource;
            return this;
        }

        public Builder<T> withFallback(ConfigurationSource fallbackSource, ConfigurationSource... otherFallbacks) {
            addFallback(fallbackSource);
            if (otherFallbacks != null) {
                Arrays.stream(otherFallbacks)
                        .forEach(this::addFallback);
            }

            return this;
        }

        public RootConfigurationProvider<T> build() {
            return new RootConfigurationProvider<>(configurationClass, configurationSource);
        }

        private void addFallback(ConfigurationSource fallbackSource) {
            this.configurationSource = MergeConfigurationSource.builder()
                    .withSource(this.configurationSource)
                    .withFallback(fallbackSource)
                    .build();
        }

    }

}
