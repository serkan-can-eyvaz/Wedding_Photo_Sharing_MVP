package com.weddingshare.media;

import com.weddingshare.event.Event;
import com.weddingshare.event.EventRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class MediaService {

    private final EventRepository eventRepository;
    private final MediaPageService mediaPageService;

    public MediaService(
            EventRepository eventRepository,
            MediaPageService mediaPageService
    ) {
        this.eventRepository = eventRepository;
        this.mediaPageService = mediaPageService;
    }

    @Transactional(readOnly = true)
    public MediaPageResponse listForOwnedEvent(UUID ownerId, UUID eventId, String cursor, int limit) {
        Event event = eventRepository.findByIdAndOwnerId(eventId, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        return mediaPageService.listForEvent(event, cursor, limit);
    }
}
