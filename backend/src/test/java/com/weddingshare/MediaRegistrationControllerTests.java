package com.weddingshare;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weddingshare.event.Event;
import com.weddingshare.event.EventRepository;
import com.weddingshare.media.Media;
import com.weddingshare.media.MediaRepository;
import com.weddingshare.storage.R2ObjectMetadata;
import com.weddingshare.storage.R2ObjectMetadataService;
import com.weddingshare.user.User;
import com.weddingshare.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:wedding_share_media;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
class MediaRegistrationControllerTests {

    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final long MAX_IMAGE_SIZE_BYTES = 20L * 1024 * 1024;
    private static final long PREVIOUS_MAX_VIDEO_SIZE_BYTES = 250L * 1024 * 1024;
    private static final long MAX_VIDEO_SIZE_BYTES = 500L * 1024 * 1024;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private R2ObjectMetadataService r2ObjectMetadataService;

    private User owner;

    @BeforeEach
    void setUp() {
        mediaRepository.deleteAll();
        eventRepository.deleteAll();
        owner = userRepository.findByEmail(ADMIN_EMAIL).orElseThrow();
    }

    @Test
    void validImageRegistersMetadataReturnedByR2WithoutAuthentication() throws Exception {
        String token = "image-registration-token";
        String storageKey = storageKey(token, "png");
        createEvent(token, true);
        when(r2ObjectMetadataService.getObjectMetadata(storageKey))
                .thenReturn(new R2ObjectMetadata("image/png", 1234));

        mockMvc.perform(registerRequest(token, storageKey, "holiday-photo.png"))
                .andExpect(status().isCreated());

        Media savedMedia = mediaRepository.findAll().get(0);
        assertThat(savedMedia.getStorageKey()).isEqualTo(storageKey);
        assertThat(savedMedia.getOriginalFilename()).isEqualTo("holiday-photo.png");
        assertThat(savedMedia.getMimeType()).isEqualTo("image/png");
        assertThat(savedMedia.getSizeBytes()).isEqualTo(1234);
        assertThat(savedMedia.getId()).isNotNull();
        assertThat(savedMedia.getCreatedAt()).isNotNull();
    }

    @Test
    void validVideoRegistersMetadataReturnedByR2() throws Exception {
        String token = "video-registration-token";
        String storageKey = storageKey(token, "mp4");
        createEvent(token, true);
        when(r2ObjectMetadataService.getObjectMetadata(storageKey))
                .thenReturn(new R2ObjectMetadata("video/mp4", MAX_VIDEO_SIZE_BYTES));

        mockMvc.perform(registerRequest(token, storageKey, "video.mp4"))
                .andExpect(status().isCreated());

        Media savedMedia = mediaRepository.findAll().get(0);
        assertThat(savedMedia.getMimeType()).isEqualTo("video/mp4");
        assertThat(savedMedia.getSizeBytes()).isEqualTo(MAX_VIDEO_SIZE_BYTES);
    }

    @Test
    void formerVideoLimitRegistersMetadataReturnedByR2() throws Exception {
        String token = "previous-video-limit-registration-token";
        String storageKey = storageKey(token, "mp4");
        createEvent(token, true);
        when(r2ObjectMetadataService.getObjectMetadata(storageKey))
                .thenReturn(new R2ObjectMetadata("video/mp4", PREVIOUS_MAX_VIDEO_SIZE_BYTES));

        mockMvc.perform(registerRequest(token, storageKey, "video.mp4"))
                .andExpect(status().isCreated());

        assertThat(mediaRepository.findAll().get(0).getSizeBytes()).isEqualTo(PREVIOUS_MAX_VIDEO_SIZE_BYTES);
    }

    @Test
    void unknownAndInactiveEventsReturnNotFound() throws Exception {
        String inactiveToken = "inactive-registration-token";
        createEvent(inactiveToken, false);

        mockMvc.perform(registerRequest("unknown-registration-token", storageKey("unknown-registration-token", "jpg"), "photo.jpg"))
                .andExpect(status().isNotFound());
        mockMvc.perform(registerRequest(inactiveToken, storageKey(inactiveToken, "jpg"), "photo.jpg"))
                .andExpect(status().isNotFound());

        verifyNoInteractions(r2ObjectMetadataService);
    }

    @Test
    void keyOutsideCurrentEventScopeOrMalformedIsRejectedBeforeR2Lookup() throws Exception {
        String token = "key-validation-token";
        createEvent(token, true);

        mockMvc.perform(registerRequest(token, storageKey("another-event", "jpg"), "photo.jpg"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(registerRequest(token, "events/" + token + "/../" + UUID.randomUUID() + ".jpg", "photo.jpg"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(r2ObjectMetadataService);
    }

    @Test
    void missingObjectReturnsNotFound() throws Exception {
        String token = "missing-object-token";
        String storageKey = storageKey(token, "jpg");
        createEvent(token, true);
        when(r2ObjectMetadataService.getObjectMetadata(storageKey))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));

        mockMvc.perform(registerRequest(token, storageKey, "photo.jpg"))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidActualMimeTypeOrSizeIsRejectedWithoutPersistingMedia() throws Exception {
        String token = "metadata-validation-token";
        String unsupportedKey = storageKey(token, "jpg");
        String oversizedImageKey = storageKey(token, "jpg");
        String oversizedVideoKey = storageKey(token, "mp4");
        createEvent(token, true);
        when(r2ObjectMetadataService.getObjectMetadata(unsupportedKey))
                .thenReturn(new R2ObjectMetadata("application/pdf", 1024));
        when(r2ObjectMetadataService.getObjectMetadata(oversizedImageKey))
                .thenReturn(new R2ObjectMetadata("image/jpeg", MAX_IMAGE_SIZE_BYTES + 1));
        when(r2ObjectMetadataService.getObjectMetadata(oversizedVideoKey))
                .thenReturn(new R2ObjectMetadata("video/mp4", MAX_VIDEO_SIZE_BYTES + 1));

        mockMvc.perform(registerRequest(token, unsupportedKey, "photo.jpg"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(registerRequest(token, oversizedImageKey, "photo.jpg"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(registerRequest(token, oversizedVideoKey, "video.mp4"))
                .andExpect(status().isBadRequest());

        assertThat(mediaRepository.count()).isZero();
    }

    @Test
    void duplicateStorageKeyReturnsConflict() throws Exception {
        String token = "duplicate-registration-token";
        String storageKey = storageKey(token, "jpg");
        createEvent(token, true);
        when(r2ObjectMetadataService.getObjectMetadata(storageKey))
                .thenReturn(new R2ObjectMetadata("image/jpeg", 1024));

        mockMvc.perform(registerRequest(token, storageKey, "photo.jpg"))
                .andExpect(status().isCreated());
        mockMvc.perform(registerRequest(token, storageKey, "photo.jpg"))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidOriginalFilenameIsRejected() throws Exception {
        String token = "filename-validation-token";
        String storageKey = storageKey(token, "jpg");
        createEvent(token, true);

        mockMvc.perform(registerRequest(token, storageKey, "folder/photo.jpg"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(registerRequest(token, storageKey, "bad\u0001name.jpg"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(registerRequest(token, storageKey, "a".repeat(256)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(r2ObjectMetadataService);
    }

    private void createEvent(String token, boolean active) {
        eventRepository.save(new Event(owner, "Media Event", LocalDate.of(2026, 9, 12), token, null, active));
    }

    private String storageKey(String token, String extension) {
        return "events/" + token + "/" + UUID.randomUUID() + "." + extension;
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder registerRequest(
            String token,
            String storageKey,
            String originalFilename
    ) throws Exception {
        return post("/api/public/events/{token}/media", token)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "storageKey", storageKey,
                        "originalFilename", originalFilename
                )));
    }
}
