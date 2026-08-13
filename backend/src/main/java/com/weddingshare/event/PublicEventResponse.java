package com.weddingshare.event;

import java.time.LocalDate;

public record PublicEventResponse(
        String name,
        LocalDate eventDate,
        String coverImageKey
) {

    public static PublicEventResponse from(Event event) {
        return new PublicEventResponse(
                event.getName(),
                event.getEventDate(),
                event.getCoverImageKey()
        );
    }
}
