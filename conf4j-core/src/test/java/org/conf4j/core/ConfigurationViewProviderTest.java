package org.conf4j.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.conf4j.core.source.ClasspathConfigurationSource;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigurationViewProviderTest {

    @Test
    public void testSubConfigurationProvider() {
        DatabaseConfiguration expectedSubConfig = new DatabaseConfiguration("localhost", "root", "secret");
        TestConfiguration expectedRootConfig = new TestConfiguration(8080, "test-service", expectedSubConfig);

        ClasspathConfigurationSource configurationSource = ClasspathConfigurationSource.builder()
                .withResourcePath("org/conf4j/core/sub-configuration-test.conf")
                .build();

        ConfigurationProvider<TestConfiguration> configurationProvider = new ConfigurationProviderBuilder<>(TestConfiguration.class)
                .withConfigurationSource(configurationSource)
                .build();

        TestConfiguration actualRootConfig = configurationProvider.get();
        assertThat(actualRootConfig).isNotNull();
        assertThat(actualRootConfig).isEqualToComparingFieldByFieldRecursively(expectedRootConfig);

        ConfigurationProvider<DatabaseConfiguration> subConfigurationProvider =
                configurationProvider.createConfigurationProvider(configuration -> configuration.database);

        assertThat(subConfigurationProvider).isNotNull();

        DatabaseConfiguration actualSubConfig = subConfigurationProvider.get();
        assertThat(actualSubConfig).isNotNull();
        assertThat(actualSubConfig).isEqualToComparingFieldByFieldRecursively(expectedSubConfig);
    }

    public static class TestConfiguration {

        int port;
        String serviceName;
        DatabaseConfiguration database;

        @JsonCreator
        TestConfiguration(@JsonProperty("port") int port, @JsonProperty("serviceName") String serviceName,
                          @JsonProperty("database") DatabaseConfiguration database) {
            this.port = port;
            this.serviceName = serviceName;
            this.database = database;
        }

    }

    static class DatabaseConfiguration {

        String host;
        String user;
        String password;

        @JsonCreator
        DatabaseConfiguration(@JsonProperty("host") String host, @JsonProperty("user") String user,
                              @JsonProperty("password") String password) {
            this.host = host;
            this.user = user;
            this.password = password;
        }

    }

}