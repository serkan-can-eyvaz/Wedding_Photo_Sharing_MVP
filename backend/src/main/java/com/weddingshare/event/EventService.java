package com.weddingshare.event;

import com.weddingshare.user.User;
import com.weddingshare.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class EventService {

    private static final int PUBLIC_TOKEN_BYTES = 32;

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public EventService(EventRepository eventRepository, UserRepository userRepository) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    public EventResponse create(UUID ownerId, EventRequest request) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        boolean active = request.active() == null || request.active();
        Event event = new Event(
                owner,
                request.name(),
                request.eventDate(),
                generatePublicToken(),
                request.coverImageKey(),
                active
        );

        return EventResponse.from(eventRepository.save(event));
    }

    public List<EventResponse> list(UUID ownerId) {
        return eventRepository.findAllByOwnerIdOrderByCreatedAtDesc(ownerId).stream()
                .map(EventResponse::from)
                .toList();
    }

    public EventResponse get(UUID ownerId, UUID eventId) {
        return EventResponse.from(findOwnedEvent(ownerId, eventId));
    }

    public EventResponse update(UUID ownerId, UUID eventId, EventRequest request) {
        Event event = findOwnedEvent(ownerId, eventId);
        boolean active = request.active() == null ? event.isActive() : request.active();
        event.update(request.name(), request.eventDate(), request.coverImageKey(), active);

        return EventResponse.from(eventRepository.save(event));
    }

    private Event findOwnedEvent(UUID ownerId, UUID eventId) {
        return eventRepository.findByIdAndOwnerId(eventId, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private String generatePublicToken() {
        byte[] bytes = new byte[PUBLIC_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
