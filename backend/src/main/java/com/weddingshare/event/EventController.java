package com.weddingshare.event;

import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;
    private final EventQrService eventQrService;

    public EventController(EventService eventService, EventQrService eventQrService) {
        this.eventService = eventService;
        this.eventQrService = eventQrService;
    }

    @PostMapping
    public ResponseEntity<EventResponse> create(Authentication authentication, @Valid @RequestBody EventRequest request) {
        EventResponse event = eventService.create(authenticatedUserId(authentication), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(event);
    }

    @GetMapping
    public List<EventResponse> list(Authentication authentication) {
        return eventService.list(authenticatedUserId(authentication));
    }

    @GetMapping("/{id}")
    public EventResponse get(Authentication authentication, @PathVariable UUID id) {
        return eventService.get(authenticatedUserId(authentication), id);
    }

    @GetMapping("/{id}/qr")
    public ResponseEntity<byte[]> downloadQr(Authentication authentication, @PathVariable UUID id) {
        byte[] png = eventQrService.generateForOwnedEvent(authenticatedUserId(authentication), id);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("event-qr.png", StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(png);
    }

    @PutMapping("/{id}")
    public EventResponse update(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody EventRequest request
    ) {
        return eventService.update(authenticatedUserId(authentication), id, request);
    }

    private UUID authenticatedUserId(Authentication authentication) {
        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }
}
