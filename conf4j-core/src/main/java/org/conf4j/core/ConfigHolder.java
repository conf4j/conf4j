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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConfigHolder<?> that = (ConfigHolder<?>) o;

        return typeSafeConfig != null ? typeSafeConfig.equals(that.typeSafeConfig) : that.typeSafeConfig == null;
    }

    @Override
    public int hashCode() {
        return typeSafeConfig != null ? typeSafeConfig.hashCode() : 0;
    }

}
