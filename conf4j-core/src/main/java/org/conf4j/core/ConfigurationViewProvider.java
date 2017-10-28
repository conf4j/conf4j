package org.conf4j.core;

import java.util.function.BiConsumer;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public class ConfigurationViewProvider<R, T> implements ConfigurationProvider<T> {

    private final ConfigurationProvider<R> parentConfigurationProvider;
    private final Function<R, T> configurationExtractor;
    private final ChangeListenersNotifier<T> changeListenersNotifier;

    ConfigurationViewProvider(ConfigurationProvider<R> parentConfigurationProvider, Function<R, T> configurationExtractor) {
        this.parentConfigurationProvider = requireNonNull(parentConfigurationProvider);
        this.configurationExtractor = requireNonNull(configurationExtractor);
        this.changeListenersNotifier = new ChangeListenersNotifier<>();

        parentConfigurationProvider.registerChangeListener(this::parentConfigurationChanged);
    }

    @Override
    public T get() {
        return configurationExtractor.apply(parentConfigurationProvider.get());
    }

    @Override
    public <C> ConfigurationProvider<C> createConfigurationProvider(Function<T, C> configurationExtractor) {
        return new ConfigurationViewProvider<>(this, configurationExtractor);
    }

    @Override
    public void registerChangeListener(BiConsumer<T, T> listener) {
        changeListenersNotifier.registerChangeListener(listener);
    }

    private void parentConfigurationChanged(R oldParentConfig, R newParentConfig) {
        T oldConfig = configurationExtractor.apply(oldParentConfig);
        T newConfig = configurationExtractor.apply(newParentConfig);
        changeListenersNotifier.notifyListenersOnConfigChangeIfNeeded(oldConfig, newConfig);
    }

}
