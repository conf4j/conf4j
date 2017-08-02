package org.conf4j.consul.source.reload;

import com.orbitz.consul.KeyValueClient;
import com.orbitz.consul.cache.KVCache;
import org.conf4j.consul.source.ConsulConfigurationSource;
import org.conf4j.core.source.reload.ReloadStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

public class ConsulWatchReloadStrategy implements ReloadStrategy {

    private static final Logger logger = LoggerFactory.getLogger(ConsulWatchReloadStrategy.class);

    private final KeyValueClient kvClient;
    private final String pathToWatch;
    private final Duration timeout;
    private KVCache kvCache;

    private ConsulWatchReloadStrategy(KeyValueClient kvClient, String pathToWatch, Duration timeout) {
        this.kvClient = requireNonNull(kvClient);
        this.pathToWatch = requireNonNull(pathToWatch);
        this.timeout = requireNonNull(timeout);
    }

    @Override
    public void start(Runnable reloadCallback) {
        kvCache = KVCache.newCache(kvClient, pathToWatch);
        kvCache.addListener(newValues -> reloadCallback.run());

        try {
            kvCache.start();
            kvCache.awaitInitialized(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (Throwable t) {
            logger.error("Unknown error occurred while initializing consul reload strategy", t);
        }
    }

    @Override
    public void stop() {
        if (kvCache != null) try {
            kvCache.stop();
        } catch (Throwable t) {
            logger.warn("Unknown error occurred while stopping consul reload strategy", t);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private ConsulConfigurationSource source;

        public Builder withConsulConfigurationSource(ConsulConfigurationSource source) {
            this.source = source;
            return this;
        }

        public ConsulWatchReloadStrategy build() {
            requireNonNull(source);
            return new ConsulWatchReloadStrategy(source.getKeyValueClient(),
                    source.getPathToWatch(), source.getReadTimeout());
        }

    }

}
