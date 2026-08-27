package com.weddingshare.media;

import com.weddingshare.event.Event;
import com.weddingshare.storage.MediaPreviewUrlService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class MediaPageService {

    public static final int DEFAULT_PAGE_SIZE = 40;
    public static final int MAX_PAGE_SIZE = 100;

    private final MediaRepository mediaRepository;
    private final MediaPreviewUrlService mediaPreviewUrlService;

    public MediaPageService(MediaRepository mediaRepository, MediaPreviewUrlService mediaPreviewUrlService) {
        this.mediaRepository = mediaRepository;
        this.mediaPreviewUrlService = mediaPreviewUrlService;
    }

    public MediaPageResponse listForEvent(Event event, String cursor, int limit) {
        if (limit < 1 || limit > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Media page size must be between 1 and " + MAX_PAGE_SIZE);
        }

        MediaCursor.CursorPosition position = MediaCursor.decode(cursor);
        PageRequest pageRequest = PageRequest.of(0, limit + 1);
        List<Media> candidates = position == null
                ? mediaRepository.findByEventIdOrderByCreatedAtDescIdDesc(event.getId(), pageRequest)
                : mediaRepository.findNextPageByEventId(event.getId(), position.createdAt(), position.mediaId(), pageRequest);
        boolean hasMore = candidates.size() > limit;
        List<Media> page = hasMore ? candidates.subList(0, limit) : candidates;
        String nextCursor = hasMore ? MediaCursor.encode(page.get(page.size() - 1)) : null;

        return new MediaPageResponse(
                page.stream()
                        .map(media -> MediaResponse.from(media, mediaPreviewUrlService.createImagePreviewUrl(media)))
                        .toList(),
                nextCursor,
                hasMore
        );
    }
}
