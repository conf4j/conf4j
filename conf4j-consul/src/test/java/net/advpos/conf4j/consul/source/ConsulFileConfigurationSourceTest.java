package net.advpos.conf4j.consul.source;

import com.orbitz.consul.Consul;
import com.orbitz.consul.KeyValueClient;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import net.advpos.conf4j.consul.source.ConsulFileConfigurationSource.Builder;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ConsulFileConfigurationSourceTest {

    private static final Logger logger = LoggerFactory.getLogger(ConsulFileConfigurationSourceTest.class);

    private static String directory = "config/";
    private static GenericContainer consulContainer;
    private static KeyValueClient keyValueClient;

    @BeforeClass
    public static void startConsulTestContainer() {
        consulContainer = new GenericContainer("consul:0.9.0")
                .withCommand("agent -dev -client 0.0.0.0")
                .withExposedPorts(8500);
        consulContainer.start();
        consulContainer.followOutput(new Slf4jLogConsumer(logger).withPrefix("consul"));

        Consul consul = Consul.builder()
                .withUrl(getConsulUrl())
                .build();
        keyValueClient = consul.keyValueClient();
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
        assertThatThrownBy(() -> createConfigurationSource(filePath, false))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testIgnoreMissingFileOption() throws Exception {
        String filePath = RandomStringUtils.randomAlphanumeric(15);
        ConsulFileConfigurationSource source = createConfigurationSource(filePath, true);

        Config config = source.getConfig();
        assertThat(config).isNotNull();
        assertThat(config).isEqualTo(ConfigFactory.empty());
    }

    private void testConfigLoaded(String filePath, String expectedMessage) {
        ConsulFileConfigurationSource source = createConfigurationSource(filePath, false);

        Config config = source.getConfig();
        assertThat(config).isNotNull();
        assertThat(config.getString("message")).isEqualTo(expectedMessage);
    }

    @AfterClass
    public static void stopConsulTestContainer() {
        if (consulContainer != null) consulContainer.stop();
    }

    private ConsulFileConfigurationSource createConfigurationSource(String filePath, boolean ignoreMissingFile) {
        Builder builder = ConsulFileConfigurationSource.builder()
                .withConfigurationFilePath(directory + filePath)
                .withConsulUrl(getConsulUrl());

        if (ignoreMissingFile) {
            builder.ignoreMissingResource();
        }

        return builder.build();
    }

    private static void putFileInConsul(String filename, String content) {
        keyValueClient.putValue(directory + filename, content);
    }

    private static String getConsulUrl() {
        return String.format("http://localhost:%s", consulContainer.getMappedPort(8500));
    }

}
