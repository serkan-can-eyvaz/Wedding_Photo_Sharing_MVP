package com.weddingshare;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.weddingshare.event.Event;
import com.weddingshare.event.EventRepository;
import com.weddingshare.media.Media;
import com.weddingshare.media.MediaRepository;
import com.weddingshare.storage.MediaPreviewUrlService;
import com.weddingshare.user.User;
import com.weddingshare.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.Instant;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminMediaControllerTests {

    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final String ADMIN_PASSWORD = "test-admin-password";

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

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private MediaPreviewUrlService mediaPreviewUrlService;

    private User owner;

    @BeforeEach
    void setUp() {
        mediaRepository.deleteAll();
        eventRepository.deleteAll();
        owner = userRepository.findByEmail(ADMIN_EMAIL).orElseThrow();
    }

    @Test
    void unauthenticatedMediaListIsRejected() throws Exception {
        mockMvc.perform(get("/api/events/{eventId}/media", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ownedEventReturnsOnlyItsMediaWithoutStorageKeyOrCredentials() throws Exception {
        Event ownedEvent = createEvent(owner, "owned-token");
        Event otherOwnedEvent = createEvent(owner, "other-owned-token");
        Media image = mediaRepository.save(new Media(
                ownedEvent,
                "events/owned-token/image.jpg",
                "image.jpg",
                "image/jpeg",
                1024
        ));
        mediaRepository.save(new Media(
                otherOwnedEvent,
                "events/other-owned-token/video.mp4",
                "video.mp4",
                "video/mp4",
                2048
        ));
        when(mediaPreviewUrlService.createImagePreviewUrl(any(Media.class)))
                .thenReturn("https://example.invalid/preview?X-Amz-Expires=900");

        JsonNode response = objectMapper.readTree(mockMvc.perform(get("/api/events/{eventId}/media", ownedEvent.getId())
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].mediaId").value(image.getId().toString()))
                .andExpect(jsonPath("$.items[0].originalFilename").value("image.jpg"))
                .andExpect(jsonPath("$.items[0].mimeType").value("image/jpeg"))
                .andExpect(jsonPath("$.items[0].sizeBytes").value(1024))
                .andExpect(jsonPath("$.items[0].previewUrl").value("https://example.invalid/preview?X-Amz-Expires=900"))
                .andExpect(jsonPath("$.items[0].storageKey").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString());

        assertThat(response.toString()).doesNotContain("test-r2-secret-key").doesNotContain("test-r2-bucket");
        assertThat(response.path("items").size()).isEqualTo(1);
    }

    @Test
    void mediaListUsesStableCursorPagesAndCreatesPreviewsOnlyForRequestedItems() throws Exception {
        Event event = createEvent(owner, "paged-token");
        Instant sameCreatedAt = Instant.parse("2026-08-21T10:15:30Z");
        for (int index = 0; index < 5; index++) {
            saveMedia(event, "events/paged-token/" + index + ".jpg", sameCreatedAt);
        }
        when(mediaPreviewUrlService.createImagePreviewUrl(any(Media.class))).thenReturn("https://example.invalid/preview");
        clearInvocations(mediaPreviewUrlService);

        JsonNode first = listPage(event.getId(), null, 2);
        JsonNode firstRepeat = listPage(event.getId(), null, 2);
        JsonNode second = listPage(event.getId(), first.path("nextCursor").asText(), 2);
        JsonNode third = listPage(event.getId(), second.path("nextCursor").asText(), 2);

        assertThat(first.path("items").size()).isEqualTo(2);
        assertThat(first.path("hasMore").asBoolean()).isTrue();
        assertThat(first.path("nextCursor").asText()).isNotBlank();
        assertThat(firstRepeat.path("items")).isEqualTo(first.path("items"));
        assertThat(second.path("items").size()).isEqualTo(2);
        assertThat(third.path("items").size()).isEqualTo(1);
        assertThat(third.path("hasMore").asBoolean()).isFalse();
        assertThat(third.path("nextCursor").isNull()).isTrue();

        List<String> allIds = new ArrayList<>();
        for (JsonNode page : List.of(first, second, third)) {
            page.path("items").forEach(item -> allIds.add(item.path("mediaId").asText()));
        }
        assertThat(allIds).hasSize(5);
        assertThat(new HashSet<>(allIds)).hasSize(5);
        verify(mediaPreviewUrlService, times(7)).createImagePreviewUrl(any(Media.class));
    }

    @Test
    void mediaListRejectsInvalidCursorAndOversizedPage() throws Exception {
        Event event = createEvent(owner, "invalid-page-token");
        String malformedCursor = Base64.getUrlEncoder().withoutPadding().encodeToString(
                "not-an-instant|00000000-0000-0000-0000-000000000000".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(get("/api/events/{eventId}/media", event.getId())
                        .queryParam("cursor", malformedCursor)
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/events/{eventId}/media", event.getId())
                        .queryParam("limit", "101")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownAndUnownedEventsReturnTheSameNotFoundResponse() throws Exception {
        User otherOwner = userRepository.save(new User(
                "other-" + UUID.randomUUID() + "@example.com",
                passwordEncoder.encode("other-password")
        ));
        Event otherEvent = createEvent(otherOwner, "other-owner-token");

        mockMvc.perform(get("/api/events/{eventId}/media", UUID.randomUUID())
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/events/{eventId}/media", otherEvent.getId())
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void configuredOriginCanReadAdminMediaWithBearerHeader() throws Exception {
        mockMvc.perform(options("/api/events/{eventId}/media", UUID.randomUUID())
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    private Event createEvent(User eventOwner, String publicToken) {
        return eventRepository.save(new Event(
                eventOwner,
                "Gallery Event",
                LocalDate.of(2026, 9, 12),
                publicToken,
                null,
                true
        ));
    }

    private Media saveMedia(Event event, String storageKey, Instant createdAt) throws Exception {
        Media media = new Media(event, storageKey, storageKey.substring(storageKey.lastIndexOf('/') + 1), "image/jpeg", 1024);
        Field field = Media.class.getDeclaredField("createdAt");
        field.setAccessible(true);
        field.set(media, createdAt);
        return mediaRepository.save(media);
    }

    private JsonNode listPage(UUID eventId, String cursor, int limit) throws Exception {
        var request = get("/api/events/{eventId}/media", eventId).queryParam("limit", Integer.toString(limit));
        if (cursor != null) {
            request.queryParam("cursor", cursor);
        }
        String body = mockMvc.perform(request.header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body);
    }

    private String adminToken() throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + ADMIN_EMAIL + "\",\"password\":\"" + ADMIN_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).path("token").asText();
    }
}
