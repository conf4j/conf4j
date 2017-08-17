package org.conf4j.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.typesafe.config.Config;
import org.apache.commons.lang3.RandomStringUtils;
import org.conf4j.core.source.FilesystemConfigurationSource;
import org.conf4j.core.source.WatchableConfigurationSource;
import org.conf4j.core.source.reload.ReloadStrategy;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

import static org.assertj.core.api.Assertions.assertThat;

public class RootConfigurationProviderTest {

    @Test
    public void testRootConfigurationProvider() {
        FilesystemConfigurationSource configurationSource = createSourceWithFile("test-configuration.conf");
        RootConfigurationProvider<TestConfiguration> provider = RootConfigurationProvider.builder(TestConfiguration.class)
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

        RootConfigurationProvider<TestConfigurationWithFallback> provider = RootConfigurationProvider.builder(TestConfigurationWithFallback.class)
                .withConfigurationSource(configurationSource)
                .withFallbacks(fallbackSource)
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
                .addFallback(specificEnvironmentSource)
                .addFallback(commonSource)
                .build();

        FallbackHierarchyConfiguration fallbackHierarchyConfiguration = provider.get();
        assertThat(fallbackHierarchyConfiguration).isNotNull();

        FallbackHierarchyConfiguration expectedConfiguration = new FallbackHierarchyConfiguration(
                "prod.db.com", "user", "password123", "service-prod");

        assertThat(fallbackHierarchyConfiguration).isEqualToComparingFieldByField(expectedConfiguration);
    }

    @Test
    public void testConfigurationReloadStrategy() throws IOException {
        File configFile = File.createTempFile(RandomStringUtils.randomAlphanumeric(12), ".conf");

        LongAdder numOfCallsToChangeListener = new LongAdder();

        AtomicReference<Runnable> reloadCallbackReference = new AtomicReference<>();
        ConfigurationProvider<TestConfiguration> provider = createConfigProviderWithReloadStrategy(configFile, reloadCallbackReference);

        provider.registerChangeListener((oldConfig, newConfig) -> numOfCallsToChangeListener.increment());

        Runnable reloadCallback = reloadCallbackReference.get();
        assertThat(reloadCallback).isNotNull();

        assertThat(numOfCallsToChangeListener.longValue()).isEqualTo(0);

        writeConfigToConfigurationFile(configFile);
        reloadCallback.run();

        assertThat(numOfCallsToChangeListener.longValue()).isEqualTo(1);
    }

    @Test
    public void testConfigurationReloadStrategyRegisteredFromWatchableConfigurationSource() throws IOException {
        File configFile = File.createTempFile(RandomStringUtils.randomAlphanumeric(12), ".conf");

        LongAdder numOfCallsToChangeListener = new LongAdder();

        AtomicReference<Runnable> reloadCallbackReference = new AtomicReference<>();
        ConfigurationProvider<TestConfiguration> provider = createConfigProviderWithWatchableConfigSource(configFile, reloadCallbackReference);

        provider.registerChangeListener((oldConfig, newConfig) -> numOfCallsToChangeListener.increment());

        Runnable reloadCallback = reloadCallbackReference.get();
        assertThat(reloadCallback).isNotNull();

        assertThat(numOfCallsToChangeListener.longValue()).isEqualTo(0);

        writeConfigToConfigurationFile(configFile);
        reloadCallback.run();

        assertThat(numOfCallsToChangeListener.longValue()).isEqualTo(1);
    }

    @Test
    public void testStopTriggeredOnReloadStrategiesOnClose() throws Exception {
        AtomicBoolean stopCalled = new AtomicBoolean(false);
        ReloadStrategy testReloadStrategy = new ReloadStrategy() {
            @Override
            public void start(Runnable reloadCallback) {}

            @Override
            public void stop() {
                stopCalled.set(true);
            }
        };

        FilesystemConfigurationSource configurationSource = createSourceWithFile("test-configuration.conf");
        RootConfigurationProvider<TestConfiguration> provider = RootConfigurationProvider.builder(TestConfiguration.class)
                .withConfigurationSource(configurationSource)
                .addReloadStrategy(testReloadStrategy)
                .build();

        assertThat(stopCalled.get()).isFalse();
        provider.close();
        assertThat(stopCalled.get()).isTrue();
    }

    @Test
    public void testJacksonIgnoresUnknownProperties() {
        FilesystemConfigurationSource configurationSource = createSourceWithFile("test-configuration.conf");
        FilesystemConfigurationSource fallbackSource = createSourceWithFile("defaults.conf");

        RootConfigurationProvider<TestConfiguration> provider = RootConfigurationProvider.builder(TestConfiguration.class)
                .withConfigurationSource(configurationSource)
                .withFallbacks(fallbackSource)
                .build();

        TestConfiguration testConfiguration = provider.get();
        assertThat(testConfiguration).isNotNull();
    }

    @Test
    public void testTypeSafeConfigObjectResolved() {
        FilesystemConfigurationSource configurationSource = createSourceWithFile("resolvable.conf");
        RootConfigurationProvider<TestConfiguration> provider = RootConfigurationProvider.builder(TestConfiguration.class)
                .withConfigurationSource(configurationSource)
                .build();

        TestConfiguration testConfiguration = provider.get();
        assertThat(testConfiguration).isNotNull();

        TestConfiguration expectedConfiguration = new TestConfiguration("conf4j", 7);
        assertThat(testConfiguration).isEqualToComparingFieldByField(expectedConfiguration);
    }

    private FilesystemConfigurationSource createSourceWithFile(String filePath) {
        return FilesystemConfigurationSource.builder()
                .withFilePath(getClass().getResource(filePath).getFile())
                .build();
    }

    private ConfigurationProvider<TestConfiguration> createConfigProviderWithReloadStrategy(File configFile,
                                                                                            AtomicReference<Runnable> reloadCallbackReference) {
        FilesystemConfigurationSource configurationSource = FilesystemConfigurationSource.builder()
                .withFilePath(configFile.getAbsolutePath())
                .build();
        return RootConfigurationProvider.builder(TestConfiguration.class)
                .withConfigurationSource(configurationSource)
                .addReloadStrategy(reloadCallbackReference::set)
                .build();
    }

    private ConfigurationProvider<TestConfiguration> createConfigProviderWithWatchableConfigSource(File configFile,
                                                                                                   AtomicReference<Runnable> reloadCallbackReference) {
        FilesystemConfigurationSource filesystemConfigurationSource= FilesystemConfigurationSource.builder()
                .withFilePath(configFile.getAbsolutePath())
                .build();

        WatchableConfigurationSource configurationSource = new WatchableConfigurationSource() {
            @Override
            public boolean shouldWatchForChange() {
                return true;
            }

            @Override
            public ReloadStrategy getReloadStrategy() {
                return reloadCallbackReference::set;
            }

            @Override
            public Config getConfig() {
                return filesystemConfigurationSource.getConfig();
            }

            @Override
            public void reload() {
                filesystemConfigurationSource.reload();
            }
        };

        return RootConfigurationProvider.builder(TestConfiguration.class)
                .withConfigurationSource(configurationSource)
                .build();
    }

    private void writeConfigToConfigurationFile(File configurationFile) throws IOException {
        FileOutputStream out = new FileOutputStream(configurationFile);
        out.write("someProperty: someValue".getBytes());
        out.close();
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
