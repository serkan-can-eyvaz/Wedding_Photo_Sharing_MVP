package com.weddingshare.storage;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.InputStream;

@Service
public class R2MediaDownloadService {

    private final S3Client r2S3Client;
    private final R2Properties r2Properties;

    public R2MediaDownloadService(S3Client r2S3Client, R2Properties r2Properties) {
        this.r2S3Client = r2S3Client;
        this.r2Properties = r2Properties;
    }

    public InputStream openObjectStream(String storageKey) {
        try {
            ResponseInputStream<GetObjectResponse> response = r2S3Client.getObject(GetObjectRequest.builder()
                    .bucket(r2Properties.bucket())
                    .key(storageKey)
                    .build());
            return response;
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
