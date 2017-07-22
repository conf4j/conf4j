package net.advpos.conf4j.core.source;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ClasspathConfigurationSourceTest {

    @Test
    public void testConfigLoadedWithHoconFile() throws Exception {
        String resourcePath = "net/advpos/conf4j/core/source/test.conf";
        testConfigLoaded(resourcePath);
    }

    @Test
    public void testConfigLoadedWithPropertiesFile() throws Exception {
        String resourcePath = "net/advpos/conf4j/core/source/test.properties";
        testConfigLoaded(resourcePath);
    }

    @Test
    public void testConfigLoadedWithJsonFile() throws Exception {
        String resourcePath = "net/advpos/conf4j/core/source/test.json";
        testConfigLoaded(resourcePath);
    }

    @Test
    public void testIllegalStateExceptionThrownWhenMissingFile() throws Exception {
        String filePath = RandomStringUtils.randomAlphanumeric(15);
        assertThatThrownBy(() -> ClasspathConfigurationSource.builder().withResourcePath(filePath).build())
                .isInstanceOf(IllegalStateException.class);

    }

    @Test
    public void testIgnoreMissingFileOption() throws Exception {
        String filePath = RandomStringUtils.randomAlphanumeric(15);
        ClasspathConfigurationSource source = ClasspathConfigurationSource.builder()
                .withResourcePath(filePath)
                .ignoreMissingResource()
                .build();

        Config config = source.getConfig();
        assertThat(config).isNotNull();
        assertThat(config).isEqualTo(ConfigFactory.empty());
    }

    private void testConfigLoaded(String resourcePath) throws Exception {
        ClasspathConfigurationSource source = ClasspathConfigurationSource.builder()
                .withResourcePath(resourcePath)
                .build();

        Config config = source.getConfig();
        assertThat(config).isNotNull();
        assertThat(config.getString("message")).isEqualTo("config-loaded-successfully");
    }

}