package net.advpos.conf4j.etcd.source;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import mousio.etcd4j.EtcdClient;
import mousio.etcd4j.responses.EtcdKeysResponse;
import net.advpos.conf4j.etcd.source.EtcdFileConfigurationSource.Builder;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class EtcdFileConfigurationSourceTest {

    private static final Logger logger = LoggerFactory.getLogger(EtcdFileConfigurationSourceTest.class);

    private static String directory = "config/";
    private static GenericContainer consulContainer;

    @BeforeClass
    public static void startConsulTestContainer() {
        consulContainer = new GenericContainer("quay.io/coreos/etcd:v3.2.4")
                .withExposedPorts(2379)
                .withCommand("etcd",
                        "--advertise-client-urls", "http://0.0.0.0:2379",
                        "--listen-client-urls", "http://0.0.0.0:2379"
                );

        consulContainer.start();
        consulContainer.followOutput(new Slf4jLogConsumer(logger).withPrefix("etcd"));
    }

    @Test
    public void testConfigLoadedWithJsonFile() throws Exception {
        String filePath = "test.json";
        String expectedMessage = RandomStringUtils.randomAlphanumeric(12);

        putFileInEtcd(filePath, String.format("{\"message\":\"%s\"}", expectedMessage));
        testConfigLoaded(filePath, expectedMessage);
    }

    @Test
    public void testConfigLoadedWithHoconFile() throws Exception {
        String filePath = "test.conf";
        String expectedMessage = RandomStringUtils.randomAlphanumeric(12);

        putFileInEtcd(filePath, String.format("message:%s", expectedMessage));
        testConfigLoaded(filePath, expectedMessage);
    }

    @Test
    public void testConfigLoadedWithPropertiesFile() throws Exception {
        String filePath = "test.properties";
        String expectedMessage = RandomStringUtils.randomAlphanumeric(12);

        putFileInEtcd(filePath, String.format("message=%s", expectedMessage));
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
        EtcdFileConfigurationSource source = createConfigurationSource(filePath, true);

        Config config = source.getConfig();
        assertThat(config).isNotNull();
        assertThat(config).isEqualTo(ConfigFactory.empty());
    }

    private void testConfigLoaded(String filePath, String expectedMessage) {
        EtcdFileConfigurationSource source = createConfigurationSource(filePath, false);

        Config config = source.getConfig();
        assertThat(config).isNotNull();
        assertThat(config.getString("message")).isEqualTo(expectedMessage);
    }

    @AfterClass
    public static void stopConsulTestContainer() {
        if (consulContainer != null) consulContainer.stop();
    }

    private EtcdFileConfigurationSource createConfigurationSource(String filePath, boolean ignoreMissingPath) {
        Builder builder = EtcdFileConfigurationSource.builder()
                .withConfigurationPath(directory + filePath)
                .addEtcdEndpoint(getEtcdUrl());

        if (ignoreMissingPath) {
            builder.ignoreMissingPath();
        }

        return builder.build();
    }

    private static void putFileInEtcd(String path, String content) {
        try(EtcdClient etcd = new EtcdClient(URI.create(getEtcdUrl()))){
            EtcdKeysResponse etcdKeysResponse = etcd.put(directory + path, content).send().get();
            String config = etcdKeysResponse.getNode().getValue();
            assertThat(config).isEqualTo(content);
        } catch (Exception e) {
            throw new RuntimeException("Unknown exception while fetching configuration from etcd", e);
        }
    }

    private static String getEtcdUrl() {
        return String.format("http://localhost:%s", consulContainer.getMappedPort(2379));
    }

}
