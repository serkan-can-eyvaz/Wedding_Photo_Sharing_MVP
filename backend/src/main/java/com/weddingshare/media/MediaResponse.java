package com.weddingshare.media;

import java.time.Instant;
import java.util.UUID;

public record MediaResponse(
        UUID mediaId,
        String originalFilename,
        String mimeType,
        long sizeBytes,
        Instant createdAt,
        String previewUrl
) {

    public static MediaResponse from(Media media, String previewUrl) {
        return new MediaResponse(
                media.getId(),
                media.getOriginalFilename(),
                media.getMimeType(),
                media.getSizeBytes(),
                media.getCreatedAt(),
                previewUrl
        );
    }
}
