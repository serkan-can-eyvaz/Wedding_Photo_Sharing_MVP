package com.weddingshare.event;

import com.weddingshare.user.User;
import com.weddingshare.user.UserRepository;
import com.weddingshare.media.MediaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class EventService {

    private static final int TOKEN_BYTES = 32;

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final MediaRepository mediaRepository;
    private final String normalizedPublicBaseUrl;
    private final SecureRandom secureRandom = new SecureRandom();

    public EventService(
            EventRepository eventRepository,
            UserRepository userRepository,
            MediaRepository mediaRepository,
            @Value("${app.public-base-url}") String publicBaseUrl
    ) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.mediaRepository = mediaRepository;
        this.normalizedPublicBaseUrl = EventQrService.normalizePublicBaseUrl(publicBaseUrl);
    }

    public EventResponse create(UUID ownerId, EventRequest request) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        boolean active = request.active() == null || request.active();
        Event event = new Event(
                owner,
                request.name(),
                request.eventDate(),
                generateToken(),
                generateUniqueViewerToken(),
                request.coverImageKey(),
                active
        );

        return response(eventRepository.save(event));
    }

    public List<EventResponse> list(UUID ownerId) {
        return eventRepository.findAllByOwnerIdOrderByCreatedAtDesc(ownerId).stream()
                .map(this::response)
                .toList();
    }

    public EventResponse get(UUID ownerId, UUID eventId) {
        return response(findOwnedEvent(ownerId, eventId));
    }

    public EventResponse update(UUID ownerId, UUID eventId, EventRequest request) {
        Event event = findOwnedEvent(ownerId, eventId);
        boolean active = request.active() == null ? event.isActive() : request.active();
        event.update(request.name(), request.eventDate(), request.coverImageKey(), active);

        return response(eventRepository.save(event));
    }

    @Transactional
    public int backfillMissingViewerTokens() {
        List<Event> missingTokens = eventRepository.findAllByViewerTokenIsNull();
        missingTokens.forEach(event -> event.setViewerToken(generateUniqueViewerToken()));
        return missingTokens.size();
    }

    private Event findOwnedEvent(UUID ownerId, UUID eventId) {
        return eventRepository.findByIdAndOwnerId(eventId, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private EventResponse response(Event event) {
        return EventResponse.from(
                event,
                normalizedPublicBaseUrl + "/e/" + event.getPublicToken(),
                normalizedPublicBaseUrl + "/gallery/" + event.getViewerToken(),
                mediaRepository.countByEventId(event.getId())
        );
    }

    private String generateUniqueViewerToken() {
        String token;
        do {
            token = generateToken();
        } while (eventRepository.existsByViewerToken(token));
        return token;
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
