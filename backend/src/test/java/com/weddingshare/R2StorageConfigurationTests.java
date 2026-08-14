package com.weddingshare;

import com.weddingshare.storage.R2Properties;
import com.weddingshare.storage.R2StorageConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import software.amazon.awssdk.services.s3.S3Client;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class R2StorageConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
            .withUserConfiguration(R2StorageConfiguration.class);

    @Autowired
    private S3Client r2S3Client;

    @Autowired
    private R2Properties r2Properties;

    @Test
    void r2ClientIsConstructedFromTestConfigurationWithoutNetworkCalls() {
        assertThat(r2S3Client).isNotNull();
        assertThat(r2Properties.endpoint()).isEqualTo("https://example.invalid");
        assertThat(r2Properties.bucket()).isEqualTo("test-r2-bucket");
    }

    @Test
    void missingR2ConfigurationPreventsClientCreation() {
        contextRunner
                .withPropertyValues(
                        "app.r2.endpoint=",
                        "app.r2.access-key-id=",
                        "app.r2.secret-access-key=",
                        "app.r2.bucket="
                )
                .run(context -> assertThat(context).hasFailed());
    }
}
