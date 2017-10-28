package org.conf4j.bean.validation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.conf4j.core.ConfigurationProvider;
import org.conf4j.core.ConfigurationProviderBuilder;
import org.conf4j.core.source.ConfigurationSource;
import org.conf4j.core.source.FilesystemConfigurationSource;
import org.junit.Test;

import javax.validation.ConstraintViolationException;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class BeanValidationExtensionTest {

    @Test
    public void testValidationChecksAreWorking() {
        ConfigurationProvider<TestConfiguration> validFileProvider = buildProvider("valid-config.conf");
        assertThat(validFileProvider.get()).isNotNull();

        assertThatThrownBy(() -> buildProvider("invalid-config.conf"))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessage("Invalid configurations");
    }

    private ConfigurationProvider<TestConfiguration> buildProvider(String filename) {
        ConfigurationSource source = FilesystemConfigurationSource.builder()
                .withFilePath(getClass().getResource(filename).getPath())
                .build();

        return new ConfigurationProviderBuilder<>(TestConfiguration.class)
                .withConfigurationSource(source)
                .build();
    }

    private static class TestConfiguration {

        @Positive
        int rangeValidation;

        @Email
        @NotNull
        String emailAddress;

        @JsonCreator
        public TestConfiguration(@JsonProperty("rangeValidation") int rangeValidation,
                                 @JsonProperty("emailAddress") String emailAddress) {
            this.rangeValidation = rangeValidation;
            this.emailAddress = emailAddress;
        }
    }

}