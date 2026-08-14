package com.weddingshare.upload;

import com.weddingshare.event.Event;
import com.weddingshare.event.PublicEventService;
import com.weddingshare.storage.R2Properties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class PresignedUploadService {

    private static final long MAX_IMAGE_SIZE_BYTES = 20L * 1024 * 1024;
    private static final long MAX_VIDEO_SIZE_BYTES = 250L * 1024 * 1024;
    private static final Duration PRESIGNED_URL_TTL = Duration.ofMinutes(15);

    private static final Map<String, UploadRule> UPLOAD_RULES = Map.of(
            "image/jpeg", new UploadRule(MAX_IMAGE_SIZE_BYTES, "jpg"),
            "image/png", new UploadRule(MAX_IMAGE_SIZE_BYTES, "png"),
            "image/heic", new UploadRule(MAX_IMAGE_SIZE_BYTES, "heic"),
            "image/heif", new UploadRule(MAX_IMAGE_SIZE_BYTES, "heif"),
            "video/mp4", new UploadRule(MAX_VIDEO_SIZE_BYTES, "mp4"),
            "video/quicktime", new UploadRule(MAX_VIDEO_SIZE_BYTES, "mov")
    );

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
        String contentType = request.contentType().trim().toLowerCase(Locale.ROOT);
        UploadRule uploadRule = uploadRuleFor(contentType, request.sizeBytes());
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

    private UploadRule uploadRuleFor(String contentType, long sizeBytes) {
        UploadRule uploadRule = UPLOAD_RULES.get(contentType);
        if (uploadRule == null || sizeBytes > uploadRule.maxSizeBytes()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        // This is request admission validation only. M8 must HEAD the uploaded object and validate actual size and content type.
        return uploadRule;
    }

    private String storageKeyFor(Event event, String extension) {
        return "events/%s/%s.%s".formatted(event.getPublicToken(), UUID.randomUUID(), extension);
    }

    private record UploadRule(long maxSizeBytes, String extension) {
    }
}
