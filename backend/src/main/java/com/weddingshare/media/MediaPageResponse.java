package com.weddingshare.media;

import java.util.List;

public record MediaPageResponse(
        List<MediaResponse> items,
        String nextCursor,
        boolean hasMore
) {
}
