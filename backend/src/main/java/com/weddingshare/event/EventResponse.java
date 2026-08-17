package com.weddingshare.event;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record EventResponse(
        UUID id,
        String name,
        LocalDate eventDate,
        String publicToken,
        String publicUrl,
        String viewerUrl,
        String coverImageKey,
        boolean active,
        Instant createdAt,
        long mediaCount
) {

    public static EventResponse from(Event event, String publicUrl, String viewerUrl, long mediaCount) {
        return new EventResponse(
                event.getId(),
                event.getName(),
                event.getEventDate(),
                event.getPublicToken(),
                publicUrl,
                viewerUrl,
                event.getCoverImageKey(),
                event.isActive(),
                event.getCreatedAt(),
                mediaCount
        );
    }
}
