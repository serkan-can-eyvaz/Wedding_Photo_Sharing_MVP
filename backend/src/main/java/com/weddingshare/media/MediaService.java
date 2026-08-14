package com.weddingshare.media;

import com.weddingshare.event.Event;
import com.weddingshare.event.EventRepository;
import com.weddingshare.storage.MediaPreviewUrlService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class MediaService {

    private final EventRepository eventRepository;
    private final MediaRepository mediaRepository;
    private final MediaPreviewUrlService mediaPreviewUrlService;

    public MediaService(
            EventRepository eventRepository,
            MediaRepository mediaRepository,
            MediaPreviewUrlService mediaPreviewUrlService
    ) {
        this.eventRepository = eventRepository;
        this.mediaRepository = mediaRepository;
        this.mediaPreviewUrlService = mediaPreviewUrlService;
    }

    @Transactional(readOnly = true)
    public List<MediaResponse> listForOwnedEvent(UUID ownerId, UUID eventId) {
        Event event = eventRepository.findByIdAndOwnerId(eventId, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        return mediaRepository.findAllByEventIdOrderByCreatedAtDesc(event.getId()).stream()
                .map(media -> MediaResponse.from(media, mediaPreviewUrlService.createImagePreviewUrl(media)))
                .toList();
    }
}
