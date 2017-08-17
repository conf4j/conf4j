package org.conf4j.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

import static java.util.Objects.requireNonNull;

class ChangeListenersNotifier<T> {

    private static final Logger logger = LoggerFactory.getLogger(ChangeListenersNotifier.class);
    private static final String CHANGE_LISTENER_CANNOT_BE_NULL = "Change listener cannot be null";

    private final List<BiConsumer<T, T>> configurationChangeListeners = new LinkedList<>();

    void registerChangeListener(BiConsumer<T, T> listener) {
        configurationChangeListeners.add(requireNonNull(listener, CHANGE_LISTENER_CANNOT_BE_NULL));
    }

    void notifyListenersOnConfigChangeIfNeeded(T oldConfig, T newConfig) {
        if (Objects.equals(oldConfig, newConfig)) {
            logger.trace("Configurations are identical - not notifying listeners");
            return;
        }

        if (configurationChangeListeners.isEmpty()) {
            logger.trace("No listener to notify");
            return;
        }

        int numOfListeners = configurationChangeListeners.size();
        logger.trace("Going to notifying {} listeners about configuration change", numOfListeners);
        configurationChangeListeners.forEach(listener -> notifyChangeListenerSafely(() -> listener.accept(oldConfig, newConfig)));
        logger.trace("{} listeners were notified about configuration change", numOfListeners);
    }

    private void notifyChangeListenerSafely(Runnable runnable) {
        try {
            runnable.run();
        } catch (Throwable t) {
            logger.error("Uncaught exception while notifying configuration change listener", t);
        }
    }

}
