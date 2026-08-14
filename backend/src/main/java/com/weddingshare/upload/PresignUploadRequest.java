package com.weddingshare.upload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PresignUploadRequest(
        @NotBlank @Size(max = 255) String filename,
        @NotBlank String contentType,
        @Positive long sizeBytes
) {
}
