package com.weddingshare;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weddingshare.event.Event;
import com.weddingshare.event.EventRepository;
import com.weddingshare.media.Media;
import com.weddingshare.media.MediaRepository;
import com.weddingshare.storage.R2MediaDownloadService;
import com.weddingshare.storage.R2ObjectMetadata;
import com.weddingshare.storage.R2ObjectMetadataService;
import com.weddingshare.user.User;
import com.weddingshare.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PublicEventRuntimeTests {

    private static final String ADMIN_EMAIL = "admin@example.com";

    @LocalServerPort
    private int port;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

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
    void unknownPublicTokenReturnsNotFoundInServletRuntime() throws IOException, InterruptedException {
        HttpResponse<String> response = get("/api/public/events/unknown-runtime-token");

        assertEquals(404, response.statusCode());
        assertGenericErrorResponse(response.body());
    }

    @Test
    void inactivePublicEventReturnsNotFoundInServletRuntime() throws IOException, InterruptedException {
        eventRepository.save(new Event(
                owner,
                "Inactive Event",
                LocalDate.of(2026, 9, 12),
                "inactive-runtime-token",
                null,
                false
        ));

        HttpResponse<String> response = get("/api/public/events/inactive-runtime-token");

        assertEquals(404, response.statusCode());
        assertGenericErrorResponse(response.body());
    }

    @Test
    void directErrorRequestRemainsUnauthorized() throws IOException, InterruptedException {
        HttpResponse<String> response = get("/error");

        assertEquals(401, response.statusCode());
        assertEquals("", response.body());
    }

    @Test
    void unauthenticatedAdminEndpointRemainsUnauthorized() throws IOException, InterruptedException {
        HttpResponse<String> response = get("/api/events");

        assertEquals(401, response.statusCode());
    }

    @Test
    void authenticatedSingleDownloadStreamsInServletRuntime() throws Exception {
        byte[] content = "runtime-download".getBytes();
        Event event = eventRepository.save(new Event(
                owner,
                "Runtime Download Event",
                LocalDate.of(2026, 9, 12),
                "runtime-download-token",
                null,
                true
        ));
        Media media = mediaRepository.save(new Media(
                event,
                "events/runtime-download-token/photo.png",
                "photo.png",
                "image/png",
                content.length
        ));
        when(r2ObjectMetadataService.getObjectMetadata(media.getStorageKey()))
                .thenReturn(new R2ObjectMetadata("image/png", content.length));
        when(r2MediaDownloadService.openObjectStream(media.getStorageKey()))
                .thenReturn(new ByteArrayInputStream(content));

        HttpRequest request = HttpRequest.newBuilder(URI.create(
                        "http://localhost:" + port + "/api/events/" + event.getId() + "/media/" + media.getId() + "/download"
                ))
                .header("Authorization", "Bearer " + adminToken())
                .GET()
                .build();

        HttpResponse<byte[]> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofByteArray());

        assertEquals(200, response.statusCode());
        assertEquals("image/png", response.headers().firstValue("Content-Type").orElseThrow());
        assertTrue(response.headers().firstValue("Content-Disposition").orElseThrow().contains("attachment"));
        assertEquals("runtime-download", new String(response.body()));
    }

    private HttpResponse<String> get(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .GET()
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String adminToken() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"email\":\"admin@example.com\",\"password\":\"test-admin-password\"}"))
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        return objectMapper.readTree(response.body()).path("token").asText();
    }

    private void assertGenericErrorResponse(String body) {
        assertFalse(body.contains("trace"));
        assertFalse(body.contains("exception"));
        assertFalse(body.contains("message"));
    }
}
