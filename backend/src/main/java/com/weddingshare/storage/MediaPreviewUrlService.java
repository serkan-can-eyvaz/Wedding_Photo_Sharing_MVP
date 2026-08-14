package com.weddingshare.storage;

import com.weddingshare.media.Media;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;

@Service
public class MediaPreviewUrlService {

    private static final Duration PREVIEW_URL_DURATION = Duration.ofMinutes(15);

    private final S3Presigner r2S3Presigner;
    private final R2Properties r2Properties;

    public MediaPreviewUrlService(S3Presigner r2S3Presigner, R2Properties r2Properties) {
        this.r2S3Presigner = r2S3Presigner;
        this.r2Properties = r2Properties;
    }

    public String createImagePreviewUrl(Media media) {
        if (!media.getMimeType().startsWith("image/")) {
            return null;
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(r2Properties.bucket())
                .key(media.getStorageKey())
                .build();

        return r2S3Presigner.presignGetObject(GetObjectPresignRequest.builder()
                        .signatureDuration(PREVIEW_URL_DURATION)
                        .getObjectRequest(getObjectRequest)
                        .build())
                .url()
                .toString();
    }
}
