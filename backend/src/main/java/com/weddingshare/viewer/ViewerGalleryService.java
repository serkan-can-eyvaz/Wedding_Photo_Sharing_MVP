package com.weddingshare.viewer;

import com.weddingshare.event.Event;
import com.weddingshare.event.EventRepository;
import com.weddingshare.media.Media;
import com.weddingshare.media.MediaDownloadService;
import com.weddingshare.media.MediaRepository;
import com.weddingshare.media.MediaResponse;
import com.weddingshare.storage.MediaPreviewUrlService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class ViewerGalleryService {

    private final EventRepository eventRepository;
    private final MediaRepository mediaRepository;
    private final MediaPreviewUrlService mediaPreviewUrlService;
    private final MediaDownloadService mediaDownloadService;

    public ViewerGalleryService(
            EventRepository eventRepository,
            MediaRepository mediaRepository,
            MediaPreviewUrlService mediaPreviewUrlService,
            MediaDownloadService mediaDownloadService
    ) {
        this.eventRepository = eventRepository;
        this.mediaRepository = mediaRepository;
        this.mediaPreviewUrlService = mediaPreviewUrlService;
        this.mediaDownloadService = mediaDownloadService;
    }

    @Transactional(readOnly = true)
    public ViewerEventResponse getEvent(String viewerToken) {
        Event event = findActiveEvent(viewerToken);
        return ViewerEventResponse.from(event, mediaRepository.countByEventId(event.getId()));
    }

    @Transactional(readOnly = true)
    public List<MediaResponse> listMedia(String viewerToken) {
        Event event = findActiveEvent(viewerToken);
        return mediaRepository.findAllByEventIdOrderByCreatedAtDesc(event.getId()).stream()
                .map(media -> MediaResponse.from(media, mediaPreviewUrlService.createImagePreviewUrl(media)))
                .toList();
    }

    public Media prepareSingleDownload(String viewerToken, UUID mediaId) {
        return mediaDownloadService.prepareSingleDownload(findActiveEvent(viewerToken), mediaId);
    }

    public MediaDownloadService.PreparedZipDownload prepareSelectedZip(String viewerToken, List<UUID> mediaIds) {
        return mediaDownloadService.prepareSelectedZip(findActiveEvent(viewerToken), mediaIds);
    }

    public MediaDownloadService.PreparedZipDownload prepareAllZip(String viewerToken) {
        return mediaDownloadService.prepareAllZip(findActiveEvent(viewerToken));
    }

    private Event findActiveEvent(String viewerToken) {
        return eventRepository.findByViewerTokenAndActiveTrue(viewerToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
