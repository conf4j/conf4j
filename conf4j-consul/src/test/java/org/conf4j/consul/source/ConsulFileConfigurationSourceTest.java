package org.conf4j.consul.source;

import com.orbitz.consul.KeyValueClient;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.commons.lang3.RandomStringUtils;
import org.conf4j.consul.StandaloneConsul;
import org.conf4j.consul.source.ConsulFileConfigurationSource.Builder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ConsulFileConfigurationSourceTest {

    private static StandaloneConsul standaloneConsul = new StandaloneConsul();
    private static KeyValueClient keyValueClient;
    private static String directory = "config/";

    @BeforeClass
    public static void startStandaloneConsul() {
        standaloneConsul.start();
        keyValueClient = standaloneConsul.getConsul().keyValueClient();
    }

    @Test
    public void testConfigLoadedWithJsonFile() throws Exception {
        String filePath = "test.json";
        String expectedMessage = RandomStringUtils.randomAlphanumeric(12);

        putFileInConsul(filePath, String.format("{\"message\":\"%s\"}", expectedMessage));
        testConfigLoaded(filePath, expectedMessage);
    }

    @Test
    public void testConfigLoadedWithHoconFile() throws Exception {
        String filePath = "test.conf";
        String expectedMessage = RandomStringUtils.randomAlphanumeric(12);

        putFileInConsul(filePath, String.format("message:%s", expectedMessage));
        testConfigLoaded(filePath, expectedMessage);
    }

    @Test
    public void testConfigLoadedWithPropertiesFile() throws Exception {
        String filePath = "test.properties";
        String expectedMessage = RandomStringUtils.randomAlphanumeric(12);

        putFileInConsul(filePath, String.format("message=%s", expectedMessage));
        testConfigLoaded(filePath, expectedMessage);
    }

    @Test
    public void testIllegalStateExceptionThrownWhenMissingFile() throws Exception {
        String filePath = RandomStringUtils.randomAlphanumeric(15);
        assertThatThrownBy(() -> createConfigurationSource(filePath, false, false))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testIgnoreMissingFileOption() throws Exception {
        String filePath = RandomStringUtils.randomAlphanumeric(15);
        ConsulFileConfigurationSource source = createConfigurationSource(filePath, true, false);

        Config config = source.getConfig();
        assertThat(config).isNotNull();
        assertThat(config).isEqualTo(ConfigFactory.empty());
    }

    @Test
    public void testReloadStrategyIsNotNullWhenCallingReloadOnChange() throws Exception {
        ConsulFileConfigurationSource source;
        String filePath = RandomStringUtils.randomAlphanumeric(15);

        source = createConfigurationSource(filePath, true, false);
        assertThat(source.shouldWatchForChange()).isFalse();
        assertThat(source.getReloadStrategy()).isNull();

        source = createConfigurationSource(filePath, true, true);
        assertThat(source.shouldWatchForChange()).isTrue();
        assertThat(source.getReloadStrategy()).isNotNull();
    }

    private void testConfigLoaded(String filePath, String expectedMessage) {
        ConsulFileConfigurationSource source = createConfigurationSource(filePath, false, false);

        Config config = source.getConfig();
        assertThat(config).isNotNull();
        assertThat(config.getString("message")).isEqualTo(expectedMessage);
    }

    @AfterClass
    public static void stopStandaloneConsul() {
        standaloneConsul.stop();
    }

    private ConsulFileConfigurationSource createConfigurationSource(String filePath, boolean ignoreMissingFile, boolean reloadOnChange) {
        Builder builder = ConsulFileConfigurationSource.builder()
                .withConfigurationFilePath(directory + filePath)
                .withConsulUrl(standaloneConsul.getConsulUrl());

        if (ignoreMissingFile) {
            builder = builder.ignoreMissingResource();
        }

        if (reloadOnChange) {
            builder = builder.reloadOnChange();
        }

        return builder.build();
    }

    private static void putFileInConsul(String filename, String content) {
        keyValueClient.putValue(directory + filename, content);
    }

}
