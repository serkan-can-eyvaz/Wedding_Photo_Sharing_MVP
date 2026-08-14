package com.weddingshare.storage;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(R2Properties.class)
public class R2StorageConfiguration {

    @Bean
    S3Client r2S3Client(R2Properties properties) {
        return S3Client.builder()
                .endpointOverride(endpoint(properties))
                .credentialsProvider(credentials(properties))
                .region(Region.of("auto"))
                .serviceConfiguration(r2ServiceConfiguration())
                .build();
    }

    @Bean
    S3Presigner r2S3Presigner(R2Properties properties) {
        return S3Presigner.builder()
                .endpointOverride(endpoint(properties))
                .credentialsProvider(credentials(properties))
                .region(Region.of("auto"))
                .serviceConfiguration(r2ServiceConfiguration())
                .build();
    }

    private static URI endpoint(R2Properties properties) {
        return URI.create(properties.endpoint());
    }

    private static StaticCredentialsProvider credentials(R2Properties properties) {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(properties.accessKeyId(), properties.secretAccessKey())
        );
    }

    private static S3Configuration r2ServiceConfiguration() {
        return S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .checksumValidationEnabled(false)
                .build();
    }
}
