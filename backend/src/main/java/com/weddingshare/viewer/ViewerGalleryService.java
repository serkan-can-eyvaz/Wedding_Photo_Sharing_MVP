package com.weddingshare.viewer;

import com.weddingshare.event.Event;
import com.weddingshare.event.EventRepository;
import com.weddingshare.media.Media;
import com.weddingshare.media.MediaDownloadService;
import com.weddingshare.media.MediaRepository;
import com.weddingshare.media.MediaPageResponse;
import com.weddingshare.media.MediaPageService;
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
    private final MediaPageService mediaPageService;
    private final MediaDownloadService mediaDownloadService;

    public ViewerGalleryService(
            EventRepository eventRepository,
            MediaRepository mediaRepository,
            MediaPageService mediaPageService,
            MediaDownloadService mediaDownloadService
    ) {
        this.eventRepository = eventRepository;
        this.mediaRepository = mediaRepository;
        this.mediaPageService = mediaPageService;
        this.mediaDownloadService = mediaDownloadService;
    }

    @Transactional(readOnly = true)
    public ViewerEventResponse getEvent(String viewerToken) {
        Event event = findActiveEvent(viewerToken);
        return ViewerEventResponse.from(event, mediaRepository.countByEventId(event.getId()));
    }

    @Transactional(readOnly = true)
    public MediaPageResponse listMedia(String viewerToken, String cursor, int limit) {
        Event event = findActiveEvent(viewerToken);
        return mediaPageService.listForEvent(event, cursor, limit);
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
