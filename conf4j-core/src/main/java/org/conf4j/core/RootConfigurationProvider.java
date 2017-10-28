package org.conf4j.core;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.typesafe.config.Config;
import org.conf4j.core.source.ConfigurationSource;
import org.conf4j.core.source.reload.ReloadStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public class RootConfigurationProvider<T> implements ConfigurationProvider<T> {

    private static final Logger logger = LoggerFactory.getLogger(RootConfigurationProvider.class);
    private static final String EMPTY_STRING = "";

    private final ChangeListenersNotifier<T> changeListenersNotifier = new ChangeListenersNotifier<>();
    private final AtomicReference<T> configurationCache = new AtomicReference<>();

    private final ObjectMapper mapper;
    private final Class<? extends T> configurationClass;
    private final ConfigurationSource configurationSource;
    private final List<ReloadStrategy> reloadStrategies;
    private final String configRootPath;

    RootConfigurationProvider(Class<? extends T> configurationClass,
                              ConfigurationSource configurationSource,
                              List<ReloadStrategy> reloadStrategies,
                              String configRootPath) {
        this.configurationClass = requireNonNull(configurationClass);
        this.configurationSource = requireNonNull(configurationSource);
        this.reloadStrategies = requireNonNull(reloadStrategies);
        this.configRootPath = requireNonNull(configRootPath);
        this.mapper = createObjectMapper();

        configurationCache.set(loadConfiguration());
        startReloadStrategies();
    }

    @Override
    public T get() {
        return configurationCache.updateAndGet(this::buildConfigObjectIfNeeded);
    }

    @Override
    public <C> ConfigurationProvider<C> createConfigurationProvider(Function<T, C> configurationExtractor) {
        return new ConfigurationViewProvider<>(this, configurationExtractor);
    }

    @Override
    public void registerChangeListener(BiConsumer<T, T> listener) {
        changeListenersNotifier.registerChangeListener(listener);
    }

    private T buildConfigObjectIfNeeded(T currentConfig) {
        if (currentConfig != null) return currentConfig;
        return loadConfiguration();
    }

    public static <T> ConfigurationProviderBuilder<T> builder(Class<? extends T> configurationClass) {
        return new ConfigurationProviderBuilder<>(configurationClass);
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
        T oldConfig = configurationCache.get();
        T newConfig = loadConfiguration();
        configurationCache.set(newConfig);

        if (Objects.equals(oldConfig, newConfig)) {
            logger.debug("Skipping notifying listeners about config reload, configurations are identical");
            return;
        }

        changeListenersNotifier.notifyListenersOnConfigChangeIfNeeded(oldConfig, newConfig);
    }

    private T loadConfiguration() {
        try {
            Config config = configurationSource.getConfig().resolve();
            if (!configRootPath.equals(EMPTY_STRING)) {
                config = config.getConfig(configRootPath);
            }

            Map<String, Object> configMap = config.root().unwrapped();
            return mapper.convertValue(configMap, configurationClass);
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

}
