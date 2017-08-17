package org.conf4j.core;

import java.util.function.BiConsumer;
import java.util.function.Function;

public interface ConfigurationProvider<T> extends AutoCloseable {

    T get();
    <C> ConfigurationProvider<C> createConfigurationProvider(Function<T, C> configurationExtractor);
    void registerChangeListener(BiConsumer<T, T> listener);

    @Override
    default void close() throws Exception {}

}
