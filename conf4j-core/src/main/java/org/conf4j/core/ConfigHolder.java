package org.conf4j.core;

import com.typesafe.config.Config;

import static java.util.Objects.requireNonNull;

class ConfigHolder<T> {

    private Config typeSafeConfig;
    private T configuration;

    ConfigHolder(Config typeSafeConfig, T configuration) {
        this.typeSafeConfig = requireNonNull(typeSafeConfig);
        this.configuration = requireNonNull(configuration);
    }

    Config getTypeSafeConfig() {
        return typeSafeConfig;
    }

    T getConfiguration() {
        return configuration;
    }

}
