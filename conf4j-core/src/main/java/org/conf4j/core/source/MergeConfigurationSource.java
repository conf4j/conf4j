package org.conf4j.core.source;

import com.typesafe.config.Config;

import static java.util.Objects.requireNonNull;

public class MergeConfigurationSource implements ConfigurationSource {

    private final ConfigurationSource source;
    private final ConfigurationSource fallbackSource;

    private MergeConfigurationSource(ConfigurationSource source, ConfigurationSource fallbackSource) {
        this.source = requireNonNull(source, "Source should not be null");
        this.fallbackSource = requireNonNull(fallbackSource, "Fallback source should not be null");
    }

    @Override
    public Config getConfig() {
        Config fallbackConfig = fallbackSource.getConfig();
        return source.getConfig()
                .withFallback(fallbackConfig);
    }

    @Override
    public void reload() {
        fallbackSource.reload();
        source.reload();
    }

    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {

        private ConfigurationSource source;
        private ConfigurationSource fallbackSource;

        private Builder() {}

        public Builder withSource(ConfigurationSource source) {
            this.source = source;
            return this;
        }

        public Builder withFallback(ConfigurationSource fallbackSource) {
            this.fallbackSource = fallbackSource;
            return this;
        }

        public MergeConfigurationSource build() {
            return new MergeConfigurationSource(source, fallbackSource);
        }

    }

}
