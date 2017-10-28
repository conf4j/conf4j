package org.conf4j.core.ext;

import com.typesafe.config.Config;

public interface ConfigurationExtension extends AutoCloseable {

    int DEFAULT_PRIORITY = 100;

    default void beforeTypeConversion(Config config, Class<?> configurationType) {}

    default void afterConfigBeanAssembly(Object resolvedBean) {}

    /**
     * Returns this extension's priority.
     * Note: Extension will be executed first with lowest priority.
     *
     * @return this extension's priority
     */
    default int getPriority() {
        return DEFAULT_PRIORITY;
    }

    default String getExtensionName() {
        return this.toString();
    }

    @Override
    default void close() throws Exception {}

}
