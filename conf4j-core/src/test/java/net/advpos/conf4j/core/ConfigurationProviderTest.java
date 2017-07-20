package net.advpos.conf4j.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.advpos.conf4j.core.source.FilesystemConfigurationSource;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigurationProviderTest {

    @Test
    public void testConfigurationProvider() {
        FilesystemConfigurationSource configurationSource = createSourceWithFile("test-configuration.conf");
        ConfigurationProvider<TestConfiguration> provider = ConfigurationProvider.builder(TestConfiguration.class)
                .withConfigurationSource(configurationSource)
                .build();

        TestConfiguration testConfiguration = provider.get();
        assertThat(testConfiguration).isNotNull();

        TestConfiguration expectedConfiguration = new TestConfiguration("conf4j", 7);
        assertThat(testConfiguration).isEqualToComparingFieldByField(expectedConfiguration);
    }

    @Test
    public void testConfigurationSourceWithFallback() {
        FilesystemConfigurationSource configurationSource = createSourceWithFile("test-configuration.conf");
        FilesystemConfigurationSource fallbackSource = createSourceWithFile("defaults.conf");

        ConfigurationProvider<TestConfigurationWithFallback> provider = ConfigurationProvider.builder(TestConfigurationWithFallback.class)
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

        ConfigurationProvider<FallbackHierarchyConfiguration> provider = ConfigurationProvider.builder(FallbackHierarchyConfiguration.class)
                .withConfigurationSource(specificServiceSource)
                .withFallback(specificEnvironmentSource, commonSource)
                .build();

        FallbackHierarchyConfiguration fallbackHierarchyConfiguration = provider.get();
        assertThat(fallbackHierarchyConfiguration).isNotNull();

        FallbackHierarchyConfiguration expectedConfiguration = new FallbackHierarchyConfiguration(
                "prod.db.com", "user", "password123", "service-prod");

        assertThat(fallbackHierarchyConfiguration).isEqualToComparingFieldByField(expectedConfiguration);
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
