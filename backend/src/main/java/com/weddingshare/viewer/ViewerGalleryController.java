package com.weddingshare.viewer;

import com.weddingshare.media.Media;
import com.weddingshare.media.MediaDownloadRequest;
import com.weddingshare.media.MediaDownloadService;
import com.weddingshare.media.MediaResponse;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/viewer/events/{viewerToken}")
public class ViewerGalleryController {

    private final ViewerGalleryService viewerGalleryService;
    private final MediaDownloadService mediaDownloadService;

    public ViewerGalleryController(ViewerGalleryService viewerGalleryService, MediaDownloadService mediaDownloadService) {
        this.viewerGalleryService = viewerGalleryService;
        this.mediaDownloadService = mediaDownloadService;
    }

    @GetMapping
    public ViewerEventResponse getEvent(@PathVariable String viewerToken) {
        return viewerGalleryService.getEvent(viewerToken);
    }

    @GetMapping("/media")
    public List<MediaResponse> listMedia(@PathVariable String viewerToken) {
        return viewerGalleryService.listMedia(viewerToken);
    }

    @GetMapping("/media/{mediaId}/download")
    public ResponseEntity<StreamingResponseBody> downloadSingle(@PathVariable String viewerToken, @PathVariable UUID mediaId) {
        Media media = viewerGalleryService.prepareSingleDownload(viewerToken, mediaId);
        HttpHeaders headers = attachmentHeaders(mediaDownloadService.safeDownloadFilename(media.getOriginalFilename()));
        headers.setContentType(parseMediaType(media.getMimeType()));
        return ResponseEntity.ok().headers(headers).body(outputStream -> mediaDownloadService.streamSingle(media, outputStream));
    }

    @PostMapping("/media/download")
    public ResponseEntity<StreamingResponseBody> downloadSelected(
            @PathVariable String viewerToken,
            @Valid @RequestBody MediaDownloadRequest request
    ) {
        return zipResponse(viewerGalleryService.prepareSelectedZip(viewerToken, request.mediaIds()), true);
    }

    @GetMapping("/media/download-all")
    public ResponseEntity<StreamingResponseBody> downloadAll(@PathVariable String viewerToken) {
        return zipResponse(viewerGalleryService.prepareAllZip(viewerToken), false);
    }

    private ResponseEntity<StreamingResponseBody> zipResponse(MediaDownloadService.PreparedZipDownload download, boolean selected) {
        HttpHeaders headers = attachmentHeaders(mediaDownloadService.zipFilename(download.event(), selected));
        headers.setContentType(MediaType.parseMediaType("application/zip"));
        return ResponseEntity.ok().headers(headers).body(outputStream -> mediaDownloadService.streamZip(download.media(), outputStream));
    }

    private HttpHeaders attachmentHeaders(String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build());
        return headers;
    }

    private MediaType parseMediaType(String mimeType) {
        try {
            return MediaType.parseMediaType(mimeType);
        } catch (IllegalArgumentException exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
