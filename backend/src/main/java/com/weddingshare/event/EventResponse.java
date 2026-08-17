package com.weddingshare.event;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record EventResponse(
        UUID id,
        String name,
        LocalDate eventDate,
        String publicToken,
        String viewerUrl,
        String coverImageKey,
        boolean active,
        Instant createdAt
) {

    public static EventResponse from(Event event, String viewerUrl) {
        return new EventResponse(
                event.getId(),
                event.getName(),
                event.getEventDate(),
                event.getPublicToken(),
                viewerUrl,
                event.getCoverImageKey(),
                event.isActive(),
                event.getCreatedAt()
        );
    }
}
