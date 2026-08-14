package com.weddingshare.media;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record MediaDownloadRequest(
        @NotEmpty List<@NotNull UUID> mediaIds
) {
}
