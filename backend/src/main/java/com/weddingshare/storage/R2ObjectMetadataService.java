package com.weddingshare.storage;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
public class R2ObjectMetadataService {

    private final S3Client r2S3Client;
    private final R2Properties r2Properties;

    public R2ObjectMetadataService(S3Client r2S3Client, R2Properties r2Properties) {
        this.r2S3Client = r2S3Client;
        this.r2Properties = r2Properties;
    }

    public R2ObjectMetadata getObjectMetadata(String storageKey) {
        try {
            HeadObjectResponse response = r2S3Client.headObject(HeadObjectRequest.builder()
                    .bucket(r2Properties.bucket())
                    .key(storageKey)
                    .build());
            if (response.contentLength() == null || !StringUtils.hasText(response.contentType())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            }

            return new R2ObjectMetadata(response.contentType(), response.contentLength());
        } catch (NoSuchKeyException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        } catch (S3Exception exception) {
            if (exception.statusCode() == HttpStatus.NOT_FOUND.value()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY);
        } catch (SdkClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY);
        }
    }
}
