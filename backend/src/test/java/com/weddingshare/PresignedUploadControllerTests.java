package com.weddingshare;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.weddingshare.event.Event;
import com.weddingshare.event.EventRepository;
import com.weddingshare.user.User;
import com.weddingshare.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PresignedUploadControllerTests {

    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final long MAX_IMAGE_SIZE_BYTES = 20L * 1024 * 1024;
    private static final long MAX_VIDEO_SIZE_BYTES = 250L * 1024 * 1024;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    private User owner;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        owner = userRepository.findByEmail(ADMIN_EMAIL).orElseThrow();
    }

    @Test
    void activeEventCanPresignImageUploadWithoutAuthentication() throws Exception {
        createEvent("active-image-token", true);

        JsonNode response = presign("active-image-token", "IMG_1234.jpg", "image/jpeg", MAX_IMAGE_SIZE_BYTES);

        assertThat(response.path("uploadUrl").asText()).startsWith("https://example.invalid/");
        assertThat(response.path("uploadUrl").asText()).contains("X-Amz-Expires=900");
        assertThat(response.path("storageKey").asText()).startsWith("events/active-image-token/").endsWith(".jpg");
        assertThat(response.path("storageKey").asText()).doesNotContain("IMG_1234.jpg");
        assertThat(response.path("expiresAt").asText()).isNotBlank();
        assertThat(response.path("requiredHeaders").path("Content-Type").asText()).isEqualTo("image/jpeg");
        assertThat(response.has("accessKeyId")).isFalse();
        assertThat(response.has("secretAccessKey")).isFalse();
        assertThat(response.has("bucket")).isFalse();
        assertThat(response.toString()).doesNotContain("test-r2-secret-key");
    }

    @Test
    void activeEventCanPresignVideoUploadWithoutAuthentication() throws Exception {
        createEvent("active-video-token", true);

        JsonNode response = presign("active-video-token", "video.mov", "video/quicktime", MAX_VIDEO_SIZE_BYTES);

        assertThat(response.path("storageKey").asText()).startsWith("events/active-video-token/").endsWith(".mov");
        assertThat(response.path("requiredHeaders").path("Content-Type").asText()).isEqualTo("video/quicktime");
    }

    @Test
    void unknownEventReturnsNotFound() throws Exception {
        mockMvc.perform(presignRequest("unknown-token", "image/jpeg", 1024))
                .andExpect(status().isNotFound());
    }

    @Test
    void inactiveEventReturnsNotFound() throws Exception {
        createEvent("inactive-token", false);

        mockMvc.perform(presignRequest("inactive-token", "image/jpeg", 1024))
                .andExpect(status().isNotFound());
    }

    @Test
    void unsupportedMimeTypeIsRejected() throws Exception {
        createEvent("unsupported-mime-token", true);

        mockMvc.perform(presignRequest("unsupported-mime-token", "application/pdf", 1024))
                .andExpect(status().isBadRequest());
    }

    @Test
    void oversizedImageIsRejected() throws Exception {
        createEvent("oversized-image-token", true);

        mockMvc.perform(presignRequest("oversized-image-token", "image/jpeg", MAX_IMAGE_SIZE_BYTES + 1))
                .andExpect(status().isBadRequest());
    }

    @Test
    void oversizedVideoIsRejected() throws Exception {
        createEvent("oversized-video-token", true);

        mockMvc.perform(presignRequest("oversized-video-token", "video/mp4", MAX_VIDEO_SIZE_BYTES + 1))
                .andExpect(status().isBadRequest());
    }

    @Test
    void generatedStorageKeysAreUnique() throws Exception {
        createEvent("unique-key-token", true);

        JsonNode first = presign("unique-key-token", "IMG_1234.jpg", "image/jpeg", 1024);
        JsonNode second = presign("unique-key-token", "IMG_1234.jpg", "image/jpeg", 1024);

        assertThat(first.path("storageKey").asText()).isNotEqualTo(second.path("storageKey").asText());
    }

    private void createEvent(String publicToken, boolean active) {
        eventRepository.save(new Event(
                owner,
                "Upload Event",
                LocalDate.of(2026, 9, 12),
                publicToken,
                null,
                active
        ));
    }

    private JsonNode presign(String token, String filename, String contentType, long sizeBytes) throws Exception {
        String response = mockMvc.perform(presignRequest(token, contentType, sizeBytes, filename))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response);
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder presignRequest(
            String token,
            String contentType,
            long sizeBytes
    ) {
        return presignRequest(token, contentType, sizeBytes, "upload.file");
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder presignRequest(
            String token,
            String contentType,
            long sizeBytes,
            String filename
    ) {
        return post("/api/public/events/{token}/uploads/presign", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"filename\":\"" + filename + "\",\"contentType\":\"" + contentType + "\",\"sizeBytes\":" + sizeBytes + "}");
    }
}
