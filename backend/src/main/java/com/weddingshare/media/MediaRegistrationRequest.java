package com.weddingshare.media;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MediaRegistrationRequest(
        @NotBlank String storageKey,
        @NotBlank @Size(max = 255) String originalFilename
) {
}
