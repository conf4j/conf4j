package net.advpos.conf4j.core.source;

import com.typesafe.config.Config;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MergeConfigurationSourceTest {

    @Test
    public void testConfigurationSourceMerging() {
        String filePath = getClass().getResource("fallback.conf").getPath();
        FilesystemConfigurationSource fallbackSource = FilesystemConfigurationSource.builder()
                .withFilePath(filePath)
                .build();

        ClasspathConfigurationSource source = ClasspathConfigurationSource.builder()
                .withResourcePath("net/advpos/conf4j/core/source/test.conf")
                .build();

        MergeConfigurationSource mergeSource = MergeConfigurationSource.builder()
                .withSource(source)
                .withFallback(fallbackSource)
                .build();

        Config config = mergeSource.getConfig();
        assertThat(config).isNotNull();
        assertThat(config.getString("message")).isEqualTo("config-loaded-successfully");
        assertThat(config.getString("override")).isEqualTo("overriding-works");
        assertThat(config.getInt("defaultProperty")).isEqualTo(555);
    }

}