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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
                .andExpect(jsonPath("$[0].mediaId").value(image.getId().toString()))
                .andExpect(jsonPath("$[0].originalFilename").value("image.jpg"))
                .andExpect(jsonPath("$[0].mimeType").value("image/jpeg"))
                .andExpect(jsonPath("$[0].sizeBytes").value(1024))
                .andExpect(jsonPath("$[0].previewUrl").value("https://example.invalid/preview?X-Amz-Expires=900"))
                .andExpect(jsonPath("$[0].storageKey").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString());

        assertThat(response.toString()).doesNotContain("test-r2-secret-key").doesNotContain("test-r2-bucket");
        assertThat(response.size()).isEqualTo(1);
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
