package com.weddingshare.media;

import org.springframework.http.HttpStatus;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
public class AdminMediaController {

    private final MediaService mediaService;
    private final MediaDownloadService mediaDownloadService;

    public AdminMediaController(MediaService mediaService, MediaDownloadService mediaDownloadService) {
        this.mediaService = mediaService;
        this.mediaDownloadService = mediaDownloadService;
    }

    @GetMapping("/{eventId}/media")
    public List<MediaResponse> list(Authentication authentication, @PathVariable UUID eventId) {
        return mediaService.listForOwnedEvent(authenticatedUserId(authentication), eventId);
    }

    @GetMapping("/{eventId}/media/{mediaId}/download")
    public org.springframework.http.ResponseEntity<StreamingResponseBody> downloadSingle(
            Authentication authentication,
            @PathVariable UUID eventId,
            @PathVariable UUID mediaId
    ) {
        Media media = mediaDownloadService.prepareSingleDownload(authenticatedUserId(authentication), eventId, mediaId);
        HttpHeaders headers = attachmentHeaders(mediaDownloadService.safeDownloadFilename(media.getOriginalFilename()));
        headers.setContentType(parseMediaType(media.getMimeType()));
        return org.springframework.http.ResponseEntity.ok()
                .headers(headers)
                .body(outputStream -> mediaDownloadService.streamSingle(media, outputStream));
    }

    @PostMapping("/{eventId}/media/download")
    public org.springframework.http.ResponseEntity<StreamingResponseBody> downloadSelected(
            Authentication authentication,
            @PathVariable UUID eventId,
            @Valid @RequestBody MediaDownloadRequest request
    ) {
        MediaDownloadService.PreparedZipDownload download = mediaDownloadService.prepareSelectedZip(
                authenticatedUserId(authentication), eventId, request.mediaIds()
        );
        return zipResponse(download, true);
    }

    @GetMapping("/{eventId}/media/download-all")
    public org.springframework.http.ResponseEntity<StreamingResponseBody> downloadAll(
            Authentication authentication,
            @PathVariable UUID eventId
    ) {
        MediaDownloadService.PreparedZipDownload download = mediaDownloadService.prepareAllZip(
                authenticatedUserId(authentication), eventId
        );
        return zipResponse(download, false);
    }

    private org.springframework.http.ResponseEntity<StreamingResponseBody> zipResponse(
            MediaDownloadService.PreparedZipDownload download,
            boolean selected
    ) {
        HttpHeaders headers = attachmentHeaders(mediaDownloadService.zipFilename(download.event(), selected));
        headers.setContentType(MediaType.parseMediaType("application/zip"));
        return org.springframework.http.ResponseEntity.ok()
                .headers(headers)
                .body(outputStream -> mediaDownloadService.streamZip(download.media(), outputStream));
    }

    private HttpHeaders attachmentHeaders(String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build());
        return headers;
    }

    private MediaType parseMediaType(String mimeType) {
        try {
            return MediaType.parseMediaType(mimeType);
        } catch (IllegalArgumentException exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private UUID authenticatedUserId(Authentication authentication) {
        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }
}
