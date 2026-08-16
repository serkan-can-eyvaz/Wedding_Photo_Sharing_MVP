package com.weddingshare;

import com.weddingshare.event.Event;
import com.weddingshare.event.EventRepository;
import com.weddingshare.user.User;
import com.weddingshare.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PublicEventRuntimeTests {

    private static final String ADMIN_EMAIL = "admin@example.com";

    @LocalServerPort
    private int port;

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

    private HttpResponse<String> get(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .GET()
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private void assertGenericErrorResponse(String body) {
        assertFalse(body.contains("trace"));
        assertFalse(body.contains("exception"));
        assertFalse(body.contains("message"));
    }
}
