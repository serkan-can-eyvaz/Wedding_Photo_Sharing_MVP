package com.weddingshare.viewer;

import com.weddingshare.event.Event;

import java.time.LocalDate;

public record ViewerEventResponse(
        String name,
        LocalDate eventDate,
        long mediaCount
) {
    static ViewerEventResponse from(Event event, long mediaCount) {
        return new ViewerEventResponse(event.getName(), event.getEventDate(), mediaCount);
    }
}
