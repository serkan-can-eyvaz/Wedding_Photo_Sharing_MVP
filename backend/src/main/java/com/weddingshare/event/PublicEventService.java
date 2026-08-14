package com.weddingshare.event;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PublicEventService {

    private final EventRepository eventRepository;

    public PublicEventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public PublicEventResponse getActiveEvent(String publicToken) {
        return PublicEventResponse.from(findActiveEvent(publicToken));
    }

    public Event findActiveEvent(String publicToken) {
        return eventRepository.findByPublicTokenAndActiveTrue(publicToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
