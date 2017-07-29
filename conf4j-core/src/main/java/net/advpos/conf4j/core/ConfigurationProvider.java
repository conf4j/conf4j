package net.advpos.conf4j.core;

import java.util.function.Function;

public abstract class ConfigurationProvider<T> {

    public abstract T get();

    public <C> SubConfigurationProvider<T, C> getSubConfigurationProvider(Function<T, C> configurationExtractor) {
        return new SubConfigurationProvider<>(this, configurationExtractor);
    }

}
