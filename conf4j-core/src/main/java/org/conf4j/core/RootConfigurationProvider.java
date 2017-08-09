package org.conf4j.core;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.typesafe.config.Config;
import org.conf4j.core.source.ConfigurationSource;
import org.conf4j.core.source.MergeConfigurationSource;
import org.conf4j.core.source.reload.ReloadStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;

public class RootConfigurationProvider<T> extends ConfigurationProvider<T> implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(RootConfigurationProvider.class);

    private final ObjectMapper mapper;
    private final Class<? extends T> configurationClass;
    private final ConfigurationSource configurationSource;
    private final List<ReloadStrategy> reloadStrategies;
    private final AtomicReference<ConfigHolder<T>> configurationCache = new AtomicReference<>();

    private RootConfigurationProvider(Class<? extends T> configurationClass,
                                      ConfigurationSource configurationSource,
                                      List<ReloadStrategy> reloadStrategies) {
        this.configurationClass = requireNonNull(configurationClass);
        this.configurationSource = requireNonNull(configurationSource);
        this.reloadStrategies = requireNonNull(reloadStrategies);
        this.mapper = createObjectMapper();
        startReloadStrategies();

        configurationCache.set(loadConfiguration());
    }

    public T get() {
        return configurationCache.updateAndGet(this::buildConfigObjectIfNeeded).getConfiguration();
    }

    public Config getConfig() {
        return configurationCache.updateAndGet(this::buildConfigObjectIfNeeded).getTypeSafeConfig();
    }

    private ConfigHolder<T> buildConfigObjectIfNeeded(ConfigHolder<T> currentConfig) {
        if (currentConfig != null) return currentConfig;
        return loadConfiguration();
    }

    public static <T> Builder<T> builder(Class<? extends T> configurationClass) {
        return new Builder<>(configurationClass);
    }

    @Override
    public void close() throws Exception {
        reloadStrategies.forEach(reloadStrategy -> {
            try {
                reloadStrategy.stop();
            } catch (Throwable t) {
                logger.warn("Unknown error while stopping reload strategy of type: {}", reloadStrategy.getClass(), t);
            }
        });
    }

    private void reload() {
        configurationSource.reload();
        ConfigHolder<T> oldConfigHolder = configurationCache.get();
        ConfigHolder<T> newConfigHolder = loadConfiguration();
        configurationCache.set(newConfigHolder);

        if (oldConfigHolder.getTypeSafeConfig().equals(newConfigHolder.getTypeSafeConfig())) {
            logger.debug("Skipping notifying listeners about config reload, configurations are identical");
            return;
        }

        T oldConfig = oldConfigHolder.getConfiguration();
        T newConfig = newConfigHolder.getConfiguration();
        notifyListenersOnConfigChangeIfNeeded(oldConfig, newConfig);
    }

    private ConfigHolder<T> loadConfiguration() {
        try {
            Config config = configurationSource.getConfig().resolve();
            Map<String, Object> configMap = config.root().unwrapped();
            T configuration = mapper.convertValue(configMap, configurationClass);
            return new ConfigHolder<>(config, configuration);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create configuration object", e);
        }
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        return mapper;
    }

    private void startReloadStrategies() {
        reloadStrategies.forEach(reloadStrategy -> reloadStrategy.start(this::reload));
    }

    public static class Builder<T> {

        private Class<? extends T> configurationClass;
        private ConfigurationSource configurationSource;
        private List<ReloadStrategy> reloadStrategies;

        private Builder(Class<? extends T> configurationClass) {
            this.configurationClass = configurationClass;
            this.reloadStrategies = new ArrayList<>();
        }

        public Builder<T> withConfigurationSource(ConfigurationSource configurationSource) {
            this.configurationSource = configurationSource;
            return this;
        }

        public Builder<T> withFallbacks(ConfigurationSource fallbackSource, ConfigurationSource... otherFallbacks) {
            addFallbackAsMergeConfigurationSource(fallbackSource);
            if (otherFallbacks != null) {
                Arrays.stream(otherFallbacks).forEach(this::addFallbackAsMergeConfigurationSource);
            }

            return this;
        }

        public Builder<T> addFallback(ConfigurationSource fallbackSource) {
            return withFallbacks(fallbackSource);
        }

        public Builder<T> addReloadStrategy(ReloadStrategy reloadStrategy) {
            requireNonNull(reloadStrategy, "Reload strategy cannot be null");
            reloadStrategies.add(reloadStrategy);
            return this;
        }

        public RootConfigurationProvider<T> build() {
            return new RootConfigurationProvider<>(configurationClass, configurationSource, reloadStrategies);
        }

        private void addFallbackAsMergeConfigurationSource(ConfigurationSource fallbackSource) {
            this.configurationSource = MergeConfigurationSource.builder()
                    .withSource(this.configurationSource)
                    .withFallback(fallbackSource)
                    .build();
        }

    }

}
