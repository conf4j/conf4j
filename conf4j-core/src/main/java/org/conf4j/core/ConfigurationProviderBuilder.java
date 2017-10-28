package org.conf4j.core;

import org.conf4j.core.source.ConfigurationSource;
import org.conf4j.core.source.MergeConfigurationSource;
import org.conf4j.core.source.WatchableConfigurationSource;
import org.conf4j.core.source.reload.ReloadStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Objects.requireNonNull;

public class ConfigurationProviderBuilder<T> {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationProviderBuilder.class);

    private Class<? extends T> configurationClass;
    private ConfigurationSource configurationSource;
    private List<ReloadStrategy> reloadStrategies;
    private String configRootPath;

    public ConfigurationProviderBuilder(Class<? extends T> configurationClass) {
        this.configurationClass = configurationClass;
        this.reloadStrategies = new ArrayList<>();
        this.configRootPath = "";
    }

    public ConfigurationProviderBuilder<T> withConfigurationSource(ConfigurationSource configurationSource) {
        this.configurationSource = configurationSource;
        addReloadStrategyIfNeeded(configurationSource);
        return this;
    }

    public ConfigurationProviderBuilder<T> withFallbacks(ConfigurationSource fallbackSource, ConfigurationSource... otherFallbacks) {
        addFallbackAsMergeConfigurationSource(fallbackSource);
        if (otherFallbacks != null) {
            Arrays.stream(otherFallbacks).forEach(this::addFallbackAsMergeConfigurationSource);
        }

        return this;
    }

    public ConfigurationProviderBuilder<T> withConfigRootPath(String configRootPath) {
        requireNonNull(configRootPath, "Config root path cannot be null");
        this.configRootPath = configRootPath;
        return this;
    }

    public ConfigurationProviderBuilder<T> addFallback(ConfigurationSource fallbackSource) {
        return withFallbacks(fallbackSource);
    }

    public ConfigurationProviderBuilder<T> addReloadStrategy(ReloadStrategy reloadStrategy) {
        requireNonNull(reloadStrategy, "Reload strategy cannot be null");
        reloadStrategies.add(reloadStrategy);
        return this;
    }

    public ConfigurationProvider<T> build() {
        return new RootConfigurationProvider<>(configurationClass, configurationSource, reloadStrategies, configRootPath);
    }

    private void addFallbackAsMergeConfigurationSource(ConfigurationSource fallbackSource) {
        this.configurationSource = MergeConfigurationSource.builder()
                .withSource(this.configurationSource)
                .withFallback(fallbackSource)
                .build();

        addReloadStrategyIfNeeded(fallbackSource);
    }

    private void addReloadStrategyIfNeeded(ConfigurationSource fallbackSource) {
        if (!WatchableConfigurationSource.class.isAssignableFrom(fallbackSource.getClass())) return;
        WatchableConfigurationSource watchableConfigSource = (WatchableConfigurationSource) fallbackSource;
        if (watchableConfigSource.shouldWatchForChange()) {
            ReloadStrategy reloadStrategy = watchableConfigSource.getReloadStrategy();

            logger.info("Registering reload strategy of type: {} from watchable configuration source",
                    reloadStrategy.getClass().getSimpleName());
            addReloadStrategy(reloadStrategy);
        }
    }

}
