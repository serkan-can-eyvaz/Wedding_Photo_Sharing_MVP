package com.weddingshare;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weddingshare.event.Event;
import com.weddingshare.event.EventRepository;
import com.weddingshare.media.Media;
import com.weddingshare.media.MediaRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminMediaDownloadControllerTests {

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
    private R2ObjectMetadataService r2ObjectMetadataService;

    @MockitoBean
    private R2MediaDownloadService r2MediaDownloadService;

    private User owner;

    @BeforeEach
    void setUp() {
        mediaRepository.deleteAll();
        eventRepository.deleteAll();
        owner = userRepository.findByEmail(ADMIN_EMAIL).orElseThrow();
    }

    @Test
    void unauthenticatedSingleDownloadIsRejected() throws Exception {
        mockMvc.perform(get("/api/events/{eventId}/media/{mediaId}/download", UUID.randomUUID(), UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ownedSingleMediaStreamsObjectWithAttachmentHeaders() throws Exception {
        Event event = createEvent(owner, "single-token");
        Media media = saveMedia(event, "events/single-token/photo.jpg", "photo.jpg", "image/jpeg");
        when(r2MediaDownloadService.openObjectStream(media.getStorageKey()))
                .thenReturn(new ByteArrayInputStream("photo-data".getBytes()));

        MockHttpServletResponse response = streamed(mockMvc.perform(get("/api/events/{eventId}/media/{mediaId}/download", event.getId(), media.getId())
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(request().asyncStarted())
                .andReturn());

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentType()).startsWith("image/jpeg");
        assertThat(response.getHeader("Content-Disposition")).contains("attachment").contains("photo.jpg");
        assertThat(response.getContentAsByteArray()).isEqualTo("photo-data".getBytes());
    }

    @Test
    void selectedZipDeduplicatesIdsAndMakesDuplicateNamesCollisionSafe() throws Exception {
        Event event = createEvent(owner, "selected-token");
        Media first = saveMedia(event, "events/selected-token/first.jpg", "file.jpg", "image/jpeg");
        Media second = saveMedia(event, "events/selected-token/second.jpg", "file.jpg", "image/jpeg");
        when(r2MediaDownloadService.openObjectStream(first.getStorageKey()))
                .thenReturn(new ByteArrayInputStream("first".getBytes()));
        when(r2MediaDownloadService.openObjectStream(second.getStorageKey()))
                .thenReturn(new ByteArrayInputStream("second".getBytes()));

        String requestBody = objectMapper.writeValueAsString(java.util.Map.of(
                "mediaIds", List.of(first.getId(), second.getId(), first.getId())
        ));
        MockHttpServletResponse response = streamed(mockMvc.perform(post("/api/events/{eventId}/media/download", event.getId())
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(request().asyncStarted())
                .andReturn());

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentType()).startsWith("application/zip");
        assertThat(zipEntryNames(response.getContentAsByteArray())).containsExactly("file.jpg", "file (2).jpg");
    }

    @Test
    void selectedZipSanitizesUnsafeEntryNames() throws Exception {
        Event event = createEvent(owner, "sanitized-token");
        Media media = saveMedia(event, "events/sanitized-token/photo.jpg", "..\\unsafe\r.jpg", "image/jpeg");
        when(r2MediaDownloadService.openObjectStream(media.getStorageKey()))
                .thenReturn(new ByteArrayInputStream("photo".getBytes()));

        String requestBody = objectMapper.writeValueAsString(java.util.Map.of("mediaIds", List.of(media.getId())));
        MockHttpServletResponse response = streamed(mockMvc.perform(post("/api/events/{eventId}/media/download", event.getId())
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(request().asyncStarted())
                .andReturn());

        assertThat(zipEntryNames(response.getContentAsByteArray())).containsExactly("__unsafe_.jpg");
    }

    @Test
    void emptySelectionIsBadRequestAndForeignMediaIsNotFoundBeforeStreaming() throws Exception {
        Event event = createEvent(owner, "empty-token");
        Event otherEvent = createEvent(owner, "other-token");
        Media foreignMedia = saveMedia(otherEvent, "events/other-token/foreign.jpg", "../foreign.jpg", "image/jpeg");

        mockMvc.perform(post("/api/events/{eventId}/media/download", event.getId())
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mediaIds\":[]}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/events/{eventId}/media/download", event.getId())
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(java.util.Map.of("mediaIds", List.of(foreignMedia.getId())))))
                .andExpect(status().isNotFound());

        User otherOwner = userRepository.save(new User("other-admin@example.com", passwordEncoder.encode("other-password")));
        Event unownedEvent = createEvent(otherOwner, "unowned-token");
        Media unownedMedia = saveMedia(unownedEvent, "events/unowned-token/private.jpg", "private.jpg", "image/jpeg");

        mockMvc.perform(get("/api/events/{eventId}/media/{mediaId}/download", unownedEvent.getId(), unownedMedia.getId())
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void allZipIncludesOnlyCurrentEventMediaAndEmptyEventIsNotFound() throws Exception {
        Event event = createEvent(owner, "all-token");
        Event otherEvent = createEvent(owner, "all-other-token");
        Media included = saveMedia(event, "events/all-token/included.jpg", "included.jpg", "image/jpeg");
        saveMedia(otherEvent, "events/all-other-token/excluded.jpg", "excluded.jpg", "image/jpeg");
        when(r2MediaDownloadService.openObjectStream(included.getStorageKey()))
                .thenReturn(new ByteArrayInputStream("included".getBytes()));

        MockHttpServletResponse response = streamed(mockMvc.perform(get("/api/events/{eventId}/media/download-all", event.getId())
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(request().asyncStarted())
                .andReturn());
        assertThat(zipEntryNames(response.getContentAsByteArray())).containsExactly("included.jpg");

        Event emptyEvent = createEvent(owner, "no-media-token");
        mockMvc.perform(get("/api/events/{eventId}/media/download-all", emptyEvent.getId())
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isNotFound());
    }

    private MockHttpServletResponse streamed(MvcResult result) throws Exception {
        result.getAsyncResult();
        return result.getResponse();
    }

    private List<String> zipEntryNames(byte[] archive) throws Exception {
        List<String> entryNames = new ArrayList<>();
        try (ZipInputStream inputStream = new ZipInputStream(new ByteArrayInputStream(archive))) {
            ZipEntry entry;
            while ((entry = inputStream.getNextEntry()) != null) {
                entryNames.add(entry.getName());
            }
        }
        return entryNames;
    }

    private Event createEvent(User eventOwner, String publicToken) {
        return eventRepository.save(new Event(
                eventOwner,
                "Download Event",
                LocalDate.of(2026, 9, 12),
                publicToken,
                null,
                true
        ));
    }

    private Media saveMedia(Event event, String storageKey, String filename, String mimeType) {
        return mediaRepository.save(new Media(event, storageKey, filename, mimeType, 1024));
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
