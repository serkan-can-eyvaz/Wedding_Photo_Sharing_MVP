package com.weddingshare.upload;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class UploadRules {

    private static final long MAX_IMAGE_SIZE_BYTES = 20L * 1024 * 1024;
    private static final long MAX_VIDEO_SIZE_BYTES = 500L * 1024 * 1024;

    private static final Map<String, UploadRule> RULES = Map.of(
            "image/jpeg", new UploadRule("image/jpeg", MAX_IMAGE_SIZE_BYTES, "jpg"),
            "image/png", new UploadRule("image/png", MAX_IMAGE_SIZE_BYTES, "png"),
            "image/heic", new UploadRule("image/heic", MAX_IMAGE_SIZE_BYTES, "heic"),
            "image/heif", new UploadRule("image/heif", MAX_IMAGE_SIZE_BYTES, "heif"),
            "video/mp4", new UploadRule("video/mp4", MAX_VIDEO_SIZE_BYTES, "mp4"),
            "video/quicktime", new UploadRule("video/quicktime", MAX_VIDEO_SIZE_BYTES, "mov")
    );

    private UploadRules() {
    }

    public static UploadRule validate(String contentType, long sizeBytes) {
        String normalizedContentType = contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
        UploadRule uploadRule = RULES.get(normalizedContentType);
        if (uploadRule == null || sizeBytes <= 0 || sizeBytes > uploadRule.maxSizeBytes()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        return uploadRule;
    }

    public static boolean isAllowedExtension(String extension) {
        return RULES.values().stream().map(UploadRule::extension).anyMatch(extension::equals);
    }

    public record UploadRule(String contentType, long maxSizeBytes, String extension) {
    }
}
