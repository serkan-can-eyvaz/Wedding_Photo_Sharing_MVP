package com.weddingshare.upload;

import java.time.Instant;
import java.util.Map;

public record PresignUploadResponse(
        String uploadUrl,
        String storageKey,
        Instant expiresAt,
        Map<String, String> requiredHeaders
) {
}
