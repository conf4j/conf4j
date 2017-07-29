package org.conf4j.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.conf4j.core.source.FilesystemConfigurationSource;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RootConfigurationProviderTest {

    @Test
    public void testRootConfigurationProvider() {
        FilesystemConfigurationSource configurationSource = createSourceWithFile("test-configuration.conf");
        RootConfigurationProvider<TestConfiguration> provider = RootConfigurationProvider.builder(TestConfiguration.class)
                .withConfigurationSource(configurationSource)
                .build();

        Config config = ConfigFactory.parseMap(ImmutableMap.of(
                "libraryName", "conf4j", "numberOfDaysInWeek", 7
        ));

        assertThat(provider.getConfig()).isEqualTo(config);

        TestConfiguration testConfiguration = provider.get();
        assertThat(testConfiguration).isNotNull();

        TestConfiguration expectedConfiguration = new TestConfiguration("conf4j", 7);
        assertThat(testConfiguration).isEqualToComparingFieldByField(expectedConfiguration);
    }

    @Test
    public void testConfigurationSourceWithFallback() {
        FilesystemConfigurationSource configurationSource = createSourceWithFile("test-configuration.conf");
        FilesystemConfigurationSource fallbackSource = createSourceWithFile("defaults.conf");

        RootConfigurationProvider<TestConfigurationWithFallback> provider = RootConfigurationProvider.builder(TestConfigurationWithFallback.class)
                .withConfigurationSource(configurationSource)
                .withFallback(fallbackSource)
                .build();

        TestConfigurationWithFallback testConfiguration = provider.get();
        assertThat(testConfiguration).isNotNull();

        TestConfigurationWithFallback expectedConfiguration = new TestConfigurationWithFallback("conf4j", 7, 12);
        assertThat(testConfiguration).isEqualToComparingFieldByField(expectedConfiguration);
    }

    @Test
    public void testConfigurationFallbackHierarchy() {
        FilesystemConfigurationSource specificServiceSource = createSourceWithFile("hierarchy/service.conf");
        FilesystemConfigurationSource specificEnvironmentSource = createSourceWithFile("hierarchy/env.conf");
        FilesystemConfigurationSource commonSource = createSourceWithFile("hierarchy/common.conf");

        RootConfigurationProvider<FallbackHierarchyConfiguration> provider = RootConfigurationProvider.builder(FallbackHierarchyConfiguration.class)
                .withConfigurationSource(specificServiceSource)
                .withFallback(specificEnvironmentSource, commonSource)
                .build();

        FallbackHierarchyConfiguration fallbackHierarchyConfiguration = provider.get();
        assertThat(fallbackHierarchyConfiguration).isNotNull();

        FallbackHierarchyConfiguration expectedConfiguration = new FallbackHierarchyConfiguration(
                "prod.db.com", "user", "password123", "service-prod");

        assertThat(fallbackHierarchyConfiguration).isEqualToComparingFieldByField(expectedConfiguration);
    }

    @Test
    public void testJacksonIgnoresUnknownProperties() {
        FilesystemConfigurationSource configurationSource = createSourceWithFile("test-configuration.conf");
        FilesystemConfigurationSource fallbackSource = createSourceWithFile("defaults.conf");

        RootConfigurationProvider<TestConfiguration> provider = RootConfigurationProvider.builder(TestConfiguration.class)
                .withConfigurationSource(configurationSource)
                .withFallback(fallbackSource)
                .build();

        TestConfiguration testConfiguration = provider.get();
        assertThat(testConfiguration).isNotNull();
    }

    private FilesystemConfigurationSource createSourceWithFile(String filePath) {
        return FilesystemConfigurationSource.builder()
                .withFilePath(getClass().getResource(filePath).getFile())
                .build();
    }

    public static class TestConfiguration {
        String libraryName;
        int numberOfDaysInWeek;

        @JsonCreator
        TestConfiguration(@JsonProperty("libraryName") String libraryName,
                          @JsonProperty("numberOfDaysInWeek") int numberOfDaysInWeek) {
            this.libraryName = libraryName;
            this.numberOfDaysInWeek = numberOfDaysInWeek;
        }
    }

    public static class TestConfigurationWithFallback {
        String libraryName;
        int numberOfDaysInWeek;
        int numberOfMonths;

        @JsonCreator
        TestConfigurationWithFallback(@JsonProperty("libraryName") String libraryName,
                          @JsonProperty("numberOfDaysInWeek") int numberOfDaysInWeek,
                          @JsonProperty("numberOfMonths") int numberOfMonths) {
            this.libraryName = libraryName;
            this.numberOfDaysInWeek = numberOfDaysInWeek;
            this.numberOfMonths = numberOfMonths;
        }
    }

    public static class FallbackHierarchyConfiguration {
        String databaseHost;
        String databaseUser;
        String databasePassword;
        String databaseSchema;

        @JsonCreator
        FallbackHierarchyConfiguration(@JsonProperty("databaseHost") String databaseHost,
                                       @JsonProperty("databaseUser") String databaseUser,
                                       @JsonProperty("databasePassword") String databasePassword,
                                       @JsonProperty("databaseSchema") String databaseSchema) {
            this.databaseHost = databaseHost;
            this.databaseUser = databaseUser;
            this.databasePassword = databasePassword;
            this.databaseSchema = databaseSchema;
        }
    }

}
