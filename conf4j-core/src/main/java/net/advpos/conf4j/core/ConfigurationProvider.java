package net.advpos.conf4j.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigRenderOptions;
import net.advpos.conf4j.core.source.ConfigurationSource;
import net.advpos.conf4j.core.source.MergeConfigurationSource;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;

public class ConfigurationProvider<T> {

    private static final ObjectMapper mapper = new ObjectMapper();

    private final Class<? extends T> configurationClass;
    private final ConfigurationSource configurationSource;
    private final AtomicReference<T> configurationCache = new AtomicReference<>();

    private ConfigurationProvider(Class<? extends T> configurationClass, ConfigurationSource configurationSource) {
        this.configurationClass = requireNonNull(configurationClass);
        this.configurationSource = requireNonNull(configurationSource);
    }

    public T get() {
        return configurationCache.updateAndGet(this::buildConfigObjectIfNeeded);
    }

    private T buildConfigObjectIfNeeded(T currentConfig) {
        if (currentConfig != null) return currentConfig;

        Config config = configurationSource.getConfig();
        config.resolve();

        String configAsJson = config.root().render(ConfigRenderOptions.concise());
        try {
            return mapper.readValue(configAsJson, configurationClass);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create configuration object", e);
        }
    }

    public static <T> Builder<T> builder(Class<? extends T> configurationClass) {
        return new Builder<>(configurationClass);
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

        public ConfigurationProvider<T> build() {
            return new ConfigurationProvider<>(configurationClass, configurationSource);
        }

        private void addFallback(ConfigurationSource fallbackSource) {
            this.configurationSource = MergeConfigurationSource.builder()
                    .withSource(this.configurationSource)
                    .withFallback(fallbackSource)
                    .build();
        }

    }

}
