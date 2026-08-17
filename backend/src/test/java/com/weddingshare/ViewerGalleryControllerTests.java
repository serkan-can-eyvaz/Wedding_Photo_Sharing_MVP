package com.weddingshare;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.weddingshare.event.Event;
import com.weddingshare.event.EventRepository;
import com.weddingshare.media.Media;
import com.weddingshare.media.MediaRepository;
import com.weddingshare.storage.MediaPreviewUrlService;
import com.weddingshare.storage.R2MediaDownloadService;
import com.weddingshare.storage.R2ObjectMetadataService;
import com.weddingshare.user.User;
import com.weddingshare.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ViewerGalleryControllerTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private EventRepository eventRepository;
    @Autowired private MediaRepository mediaRepository;
    @Autowired private UserRepository userRepository;

    @MockitoBean private MediaPreviewUrlService mediaPreviewUrlService;
    @MockitoBean private R2ObjectMetadataService r2ObjectMetadataService;
    @MockitoBean private R2MediaDownloadService r2MediaDownloadService;

    private User owner;

    @BeforeEach
    void setUp() {
        mediaRepository.deleteAll();
        eventRepository.deleteAll();
        owner = userRepository.findByEmail("admin@example.com").orElseThrow();
    }

    @Test
    void activeViewerTokenListsOnlyItsMediaWithoutAuthenticationOrInternalFields() throws Exception {
        Event event = event("guest-upload-token", "viewer-gallery-token", true);
        Media image = mediaRepository.save(new Media(event, "events/guest-upload-token/photo.jpg", "photo.jpg", "image/jpeg", 1024));
        Event other = event("other-upload-token", "other-viewer-token", true);
        mediaRepository.save(new Media(other, "events/other-upload-token/private.mp4", "private.mp4", "video/mp4", 2048));
        when(mediaPreviewUrlService.createImagePreviewUrl(any(Media.class))).thenReturn("https://example.invalid/preview");

        mockMvc.perform(get("/api/viewer/events/{token}", "viewer-gallery-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Viewer Event"))
                .andExpect(jsonPath("$.mediaCount").value(1))
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.publicToken").doesNotExist())
                .andExpect(jsonPath("$.viewerToken").doesNotExist());

        mockMvc.perform(get("/api/viewer/events/{token}/media", "viewer-gallery-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].mediaId").value(image.getId().toString()))
                .andExpect(jsonPath("$[0].previewUrl").value("https://example.invalid/preview"))
                .andExpect(jsonPath("$[0].storageKey").doesNotExist());
    }

    @Test
    void unknownInactiveAndPublicTokensAreIndistinguishableNotFound() throws Exception {
        event("active-public-token", "inactive-viewer-token", false);

        for (String token : List.of("unknown-viewer-token", "inactive-viewer-token", "active-public-token")) {
            mockMvc.perform(get("/api/viewer/events/{token}", token))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    void viewerCanDownloadOwnMediaButCannotAccessAnotherEventsMedia() throws Exception {
        Event event = event("upload-token", "viewer-token", true);
        Media media = mediaRepository.save(new Media(event, "events/upload-token/photo.jpg", "photo.jpg", "image/jpeg", 1024));
        Event other = event("other-upload-token", "other-viewer-token", true);
        Media foreign = mediaRepository.save(new Media(other, "events/other-upload-token/private.jpg", "private.jpg", "image/jpeg", 1024));
        when(r2MediaDownloadService.openObjectStream(media.getStorageKey())).thenReturn(new ByteArrayInputStream("photo".getBytes()));

        MvcResult result = mockMvc.perform(get("/api/viewer/events/{token}/media/{mediaId}/download", "viewer-token", media.getId()))
                .andExpect(request().asyncStarted())
                .andReturn();
        result.getAsyncResult();
        MockHttpServletResponse response = result.getResponse();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsByteArray()).isEqualTo("photo".getBytes());

        mockMvc.perform(get("/api/viewer/events/{token}/media/{mediaId}/download", "viewer-token", foreign.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void selectedDownloadRejectsEmptyAndCrossEventMediaBeforeStreaming() throws Exception {
        Event event = event("upload-token", "viewer-token", true);
        Event other = event("other-upload-token", "other-viewer-token", true);
        Media foreign = mediaRepository.save(new Media(other, "events/other-upload-token/private.jpg", "private.jpg", "image/jpeg", 1024));

        mockMvc.perform(post("/api/viewer/events/{token}/media/download", "viewer-token")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"mediaIds\":[]}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/viewer/events/{token}/media/download", "viewer-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("mediaIds", List.of(foreign.getId())))))
                .andExpect(status().isNotFound());
    }

    @Test
    void configuredOriginCanAccessViewerApi() throws Exception {
        mockMvc.perform(options("/api/viewer/events/example-token")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    private Event event(String publicToken, String viewerToken, boolean active) {
        return eventRepository.save(new Event(owner, "Viewer Event", LocalDate.of(2026, 9, 12), publicToken, viewerToken, null, active));
    }
}
