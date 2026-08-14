package com.weddingshare.upload;

import com.weddingshare.event.Event;
import com.weddingshare.event.PublicEventService;
import com.weddingshare.storage.R2Properties;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class PresignedUploadService {

    private static final Duration PRESIGNED_URL_TTL = Duration.ofMinutes(15);

    private final PublicEventService publicEventService;
    private final S3Presigner r2S3Presigner;
    private final R2Properties r2Properties;

    public PresignedUploadService(
            PublicEventService publicEventService,
            S3Presigner r2S3Presigner,
            R2Properties r2Properties
    ) {
        this.publicEventService = publicEventService;
        this.r2S3Presigner = r2S3Presigner;
        this.r2Properties = r2Properties;
    }

    public PresignUploadResponse createPresignedUpload(String publicToken, PresignUploadRequest request) {
        Event event = publicEventService.findActiveEvent(publicToken);
        UploadRules.UploadRule uploadRule = UploadRules.validate(request.contentType(), request.sizeBytes());
        String contentType = uploadRule.contentType();
        String storageKey = storageKeyFor(event, uploadRule.extension());

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(r2Properties.bucket())
                .key(storageKey)
                .contentType(contentType)
                .build();
        PresignedPutObjectRequest presignedRequest = r2S3Presigner.presignPutObject(
                PutObjectPresignRequest.builder()
                        .signatureDuration(PRESIGNED_URL_TTL)
                        .putObjectRequest(putObjectRequest)
                        .build()
        );

        return new PresignUploadResponse(
                presignedRequest.url().toString(),
                storageKey,
                Instant.now().plus(PRESIGNED_URL_TTL),
                Map.of("Content-Type", contentType)
        );
    }

    private String storageKeyFor(Event event, String extension) {
        return "events/%s/%s.%s".formatted(event.getPublicToken(), UUID.randomUUID(), extension);
    }
}
