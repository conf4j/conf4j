package net.advpos.conf4j.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public abstract class ConfigurationProvider<T> {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationProvider.class);

    private static final String CHANGE_LISTENER_CANNOT_BE_NULL = "Change listener cannot be null";

    private final List<BiConsumer<T, T>> configurationChangeListeners = new LinkedList<>();
    private final List<Consumer<T>> newConfigurationListeners = new LinkedList<>();

    public abstract T get();

    public <C> SubConfigurationProvider<T, C> getSubConfigurationProvider(Function<T, C> configurationExtractor) {
        return new SubConfigurationProvider<>(this, configurationExtractor);
    }

    public void registerChangeListener(BiConsumer<T, T> listener) {
        configurationChangeListeners.add(requireNonNull(listener, CHANGE_LISTENER_CANNOT_BE_NULL));
    }

    public void registerChangeListener(Consumer<T> listener) {
        newConfigurationListeners.add(requireNonNull(listener, "Change listener cannot be null"));
    }

    protected void notifyListenersOnConfigChangeIfNeeded(T oldConfig, T newConfig) {
        if (newConfig.equals(oldConfig)) {
            logger.trace("Configurations are identical - not notifying listeners");
            return;
        }

        int numOfListeners = configurationChangeListeners.size() + newConfigurationListeners.size();
        if (numOfListeners == 0) {
            logger.trace("No listener to notify");
            return;
        }

        logger.trace("Going to notifying {} listeners about configuration change", numOfListeners);
        configurationChangeListeners.forEach(listener -> listener.accept(oldConfig, newConfig));
        newConfigurationListeners.forEach(listener -> listener.accept(newConfig));
        logger.trace("{} listeners were notified about configuration change", numOfListeners);
    }

}
