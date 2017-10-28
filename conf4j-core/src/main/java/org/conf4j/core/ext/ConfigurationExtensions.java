package org.conf4j.core.ext;

import com.google.common.collect.Streams;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

public class ConfigurationExtensions {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationExtensions.class);

    private final List<ConfigurationExtension> extensions;

    public ConfigurationExtensions() {
        this.extensions = loadExtentions();
    }

    public void beforeTypeConversion(Config config, Class<?> configurationType) {
        extensions.forEach(extension -> extension.beforeTypeConversion(config, configurationType));
    }

    public void afterConfigBeanAssembly(Object configurationBean) {
        extensions.forEach(extension -> extension.afterConfigBeanAssembly(configurationBean));
    }

    public void closeExtentions() {
        extensions.forEach(extension -> {
            try {
                extension.close();
            } catch (Exception e) {
                logger.error("Unknown error thrown while closing extension: {}", extension.getExtensionName(), e);
            }
        });
    }

    private List<ConfigurationExtension> loadExtentions() {
        ServiceLoader<ConfigurationExtension> extensionServiceLoader = ServiceLoader.load(ConfigurationExtension.class);
        List<ConfigurationExtension> sortedExtentions = Streams.stream(extensionServiceLoader.iterator())
                .sorted(Comparator.comparing(ConfigurationExtension::getPriority))
                .peek(extension -> logger.debug("Adding extension: {}", extension.getExtensionName()))
                .collect(Collectors.toList());

        return Collections.unmodifiableList(sortedExtentions);
    }

}
