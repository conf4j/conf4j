package org.conf4j.consul.source.reload;

import com.orbitz.consul.KeyValueClient;
import org.apache.commons.lang3.RandomStringUtils;
import org.conf4j.consul.StandaloneConsul;
import org.conf4j.consul.source.ConsulFileConfigurationSource;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class ConsulWatchReloadStrategyTest {

    private static StandaloneConsul standaloneConsul = new StandaloneConsul();
    private static KeyValueClient keyValueClient;
    private static String directory = "config/";

    @BeforeClass
    public static void startStandaloneConsul() {
        standaloneConsul.start();
        keyValueClient = standaloneConsul.getConsul().keyValueClient();
    }

    @Test
    public void testReloadCalledOnChangeInConsul() {
        String filename = getTestFilename();
        String fieldName = RandomStringUtils.randomAlphanumeric(12);

        putConfigInConsul(filename, fieldName, RandomStringUtils.randomAlphanumeric(12));

        ConsulFileConfigurationSource configurationSource = createConfigurationSource(filename);
        ConsulWatchReloadStrategy reloadStrategy = ConsulWatchReloadStrategy.builder()
                .withConsulConfigurationSource(configurationSource)
                .build();

        LongAdder numberOfReloads = new LongAdder();

        try {
            reloadStrategy.start(numberOfReloads::increment);

            // KVCache calls his listeners after it starts
            assertThat(numberOfReloads.longValue()).isLessThanOrEqualTo(1);

            putConfigInConsul(filename, fieldName, RandomStringUtils.randomAlphanumeric(12));

            await("Reload called after change in consul")
                    .atMost(5, TimeUnit.SECONDS)
                    .until(() -> numberOfReloads.longValue() > 1);
        } finally {
            reloadStrategy.stop();
        }
    }

    @AfterClass
    public static void stopStandaloneConsul() {
        standaloneConsul.stop();
    }

    private ConsulFileConfigurationSource createConfigurationSource(String filename) {
        return ConsulFileConfigurationSource.builder()
                .withConfigurationFilePath(directory + filename)
                .withConsulUrl(standaloneConsul.getConsulUrl())
                .build();
    }

    private void putConfigInConsul(String filename, String key, String value) {
        String content = String.format("%s: %s", key, value);
        keyValueClient.putValue(directory + filename, content);
    }

    private String getTestFilename() {
        return RandomStringUtils.randomAlphanumeric(12) + ".conf";
    }
}