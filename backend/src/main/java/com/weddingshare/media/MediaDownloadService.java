package com.weddingshare.media;

import com.weddingshare.event.Event;
import com.weddingshare.event.EventRepository;
import com.weddingshare.storage.R2MediaDownloadService;
import com.weddingshare.storage.R2ObjectMetadataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class MediaDownloadService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MediaDownloadService.class);
    private static final String FALLBACK_FILENAME = "download";

    private final EventRepository eventRepository;
    private final MediaRepository mediaRepository;
    private final R2ObjectMetadataService r2ObjectMetadataService;
    private final R2MediaDownloadService r2MediaDownloadService;

    public MediaDownloadService(
            EventRepository eventRepository,
            MediaRepository mediaRepository,
            R2ObjectMetadataService r2ObjectMetadataService,
            R2MediaDownloadService r2MediaDownloadService
    ) {
        this.eventRepository = eventRepository;
        this.mediaRepository = mediaRepository;
        this.r2ObjectMetadataService = r2ObjectMetadataService;
        this.r2MediaDownloadService = r2MediaDownloadService;
    }

    public Media prepareSingleDownload(UUID ownerId, UUID eventId, UUID mediaId) {
        Event event = findOwnedEvent(ownerId, eventId);
        return prepareSingleDownload(event, mediaId);
    }

    public Media prepareSingleDownload(Event event, UUID mediaId) {
        Media media = mediaRepository.findByIdAndEventId(mediaId, event.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        verifyObjectsAvailable(List.of(media));
        return media;
    }

    public PreparedZipDownload prepareSelectedZip(UUID ownerId, UUID eventId, List<UUID> requestedMediaIds) {
        if (requestedMediaIds == null || requestedMediaIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        Event event = findOwnedEvent(ownerId, eventId);
        return prepareSelectedZip(event, requestedMediaIds);
    }

    public PreparedZipDownload prepareSelectedZip(Event event, List<UUID> requestedMediaIds) {
        if (requestedMediaIds == null || requestedMediaIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        List<UUID> mediaIds = new ArrayList<>(new LinkedHashSet<>(requestedMediaIds));
        List<Media> foundMedia = mediaRepository.findAllByEventIdAndIdIn(event.getId(), mediaIds);
        if (foundMedia.size() != mediaIds.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        Map<UUID, Media> mediaById = new HashMap<>();
        foundMedia.forEach(media -> mediaById.put(media.getId(), media));
        List<Media> orderedMedia = mediaIds.stream().map(mediaById::get).toList();
        verifyObjectsAvailable(orderedMedia);
        return new PreparedZipDownload(event, orderedMedia);
    }

    public PreparedZipDownload prepareAllZip(UUID ownerId, UUID eventId) {
        Event event = findOwnedEvent(ownerId, eventId);
        return prepareAllZip(event);
    }

    public PreparedZipDownload prepareAllZip(Event event) {
        List<Media> media = mediaRepository.findAllByEventIdOrderByCreatedAtDesc(event.getId());
        if (media.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        verifyObjectsAvailable(media);
        return new PreparedZipDownload(event, media);
    }

    public void streamSingle(Media media, OutputStream outputStream) throws IOException {
        try (InputStream inputStream = r2MediaDownloadService.openObjectStream(media.getStorageKey())) {
            inputStream.transferTo(outputStream);
            outputStream.flush();
        } catch (Exception exception) {
            LOGGER.error("Single media download stream failed", exception);
            throw new IOException("Unable to stream media download", exception);
        }
    }

    public void streamZip(List<Media> media, OutputStream outputStream) throws IOException {
        ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream, StandardCharsets.UTF_8);
        Set<String> usedNames = new HashSet<>();

        try {
            for (Media item : media) {
                String entryName = uniqueZipEntryName(item.getOriginalFilename(), usedNames);
                try (InputStream inputStream = r2MediaDownloadService.openObjectStream(item.getStorageKey())) {
                    zipOutputStream.putNextEntry(new ZipEntry(entryName));
                    inputStream.transferTo(zipOutputStream);
                    zipOutputStream.closeEntry();
                }
            }
            zipOutputStream.finish();
            outputStream.flush();
        } catch (Exception exception) {
            try {
                zipOutputStream.closeEntry();
            } catch (IOException closeException) {
                exception.addSuppressed(closeException);
            }
            LOGGER.error("Media ZIP download stream failed", exception);
            throw new IOException("Unable to stream media archive", exception);
        }
    }

    public String safeDownloadFilename(String originalFilename) {
        String sanitized = sanitizeFilename(originalFilename);
        return sanitized.isBlank() ? FALLBACK_FILENAME : sanitized;
    }

    public String zipFilename(Event event, boolean selected) {
        String prefix = safeDownloadFilename(event.getName());
        return prefix + (selected ? "-selected-media.zip" : "-media.zip");
    }

    private Event findOwnedEvent(UUID ownerId, UUID eventId) {
        return eventRepository.findByIdAndOwnerId(eventId, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void verifyObjectsAvailable(Collection<Media> media) {
        media.forEach(item -> r2ObjectMetadataService.getObjectMetadata(item.getStorageKey()));
    }

    private String uniqueZipEntryName(String originalFilename, Set<String> usedNames) {
        String sanitized = safeDownloadFilename(originalFilename);
        String candidate = sanitized;
        int suffix = 2;
        while (!usedNames.add(candidate.toLowerCase(Locale.ROOT))) {
            candidate = appendSuffix(sanitized, suffix++);
        }
        return candidate;
    }

    private String sanitizeFilename(String originalFilename) {
        if (originalFilename == null) {
            return FALLBACK_FILENAME;
        }

        String sanitized = originalFilename
                .replaceAll("[\\\\/\\p{Cntrl}]", "_")
                .replace("..", "_")
                .trim();
        if (sanitized.isBlank() || sanitized.equals(".") || sanitized.equals("_")) {
            return FALLBACK_FILENAME;
        }
        return sanitized;
    }

    private String appendSuffix(String filename, int suffix) {
        int extensionIndex = filename.lastIndexOf('.');
        if (extensionIndex > 0 && extensionIndex < filename.length() - 1) {
            return filename.substring(0, extensionIndex) + " (" + suffix + ")" + filename.substring(extensionIndex);
        }
        return filename + " (" + suffix + ")";
    }

    public record PreparedZipDownload(Event event, List<Media> media) {
    }
}
