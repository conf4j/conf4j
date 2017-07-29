package org.conf4j.core.source;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FilesystemConfigurationSourceTest {

    @Test
    public void testConfigLoadedWithHoconFile() throws Exception {
        String filePath = getClass().getResource("test.conf").getPath();
        testConfigLoaded(filePath);
    }

    @Test
    public void testConfigLoadedWithPropertiesFile() throws Exception {
        String filePath = getClass().getResource("test.properties").getPath();
        testConfigLoaded(filePath);
    }

    @Test
    public void testConfigLoadedWithJsonFile() throws Exception {
        String filePath = getClass().getResource("test.json").getPath();
        testConfigLoaded(filePath);
    }

    @Test
    public void testIllegalStateExceptionThrownWhenMissingFile() throws Exception {
        String filePath = RandomStringUtils.randomAlphanumeric(15);
        assertThatThrownBy(() -> FilesystemConfigurationSource.builder().withFilePath(filePath).build())
                .isInstanceOf(IllegalStateException.class);

    }

    @Test
    public void testIgnoreMissingFileOption() throws Exception {
        String filePath = RandomStringUtils.randomAlphanumeric(15);
        FilesystemConfigurationSource source = FilesystemConfigurationSource.builder()
                .withFilePath(filePath)
                .ignoreMissingFile()
                .build();

        Config config = source.getConfig();
        assertThat(config).isNotNull();
        assertThat(config).isEqualTo(ConfigFactory.empty());
    }

    private void testConfigLoaded(String filePath) {
        FilesystemConfigurationSource source = FilesystemConfigurationSource.builder()
                .withFilePath(filePath)
                .build();

        Config config = source.getConfig();
        assertThat(config).isNotNull();
        assertThat(config.getString("message")).isEqualTo("config-loaded-successfully");
    }

}