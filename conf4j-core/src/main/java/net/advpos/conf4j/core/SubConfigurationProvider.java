package net.advpos.conf4j.core;

import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public class SubConfigurationProvider<R, T> extends ConfigurationProvider<T> {

    private final ConfigurationProvider<R> parentConfigurationProvider;
    private final Function<R, T> configurationExtractor;

    SubConfigurationProvider(ConfigurationProvider<R> parentConfigurationProvider, Function<R, T> configurationExtractor) {
        this.parentConfigurationProvider = requireNonNull(parentConfigurationProvider);
        this.configurationExtractor = requireNonNull(configurationExtractor);
    }

    @Override
    public T get() {
        return configurationExtractor.apply(parentConfigurationProvider.get());
    }

}
