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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EventControllerTests {

    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final String ADMIN_PASSWORD = "test-admin-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void clearEvents() {
        eventRepository.deleteAll();
    }

    @Test
    void unauthenticatedEventsRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/events"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedCreateGeneratesIndependentPublicTokenAndDefaultsActiveToTrue() throws Exception {
        JsonNode created = createAdminEvent("Ayşe & Mehmet", "2026-09-12", null, null);

        assertThat(created.path("publicToken").asText()).isNotEqualTo(created.path("id").asText());
        assertThat(created.path("publicToken").asText()).doesNotContain("=");
        assertThat(created.path("active").asBoolean()).isTrue();
    }

    @Test
    void listReturnsOnlyEventsOwnedByAuthenticatedAdmin() throws Exception {
        JsonNode adminEvent = createAdminEvent("Admin Event", "2026-09-12", null, true);
        User otherUser = userRepository.save(new User("other-" + UUID.randomUUID() + "@example.com", passwordEncoder.encode("other-password")));
        Event otherEvent = eventRepository.save(new Event(
                otherUser,
                "Other Event",
                LocalDate.of(2026, 10, 1),
                "other-" + UUID.randomUUID(),
                null,
                true
        ));

        JsonNode events = objectMapper.readTree(mockMvc.perform(get("/api/events")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());

        List<String> eventIds = StreamSupport.stream(events.spliterator(), false)
                .map(node -> node.path("id").asText())
                .toList();

        assertThat(eventIds)
                .contains(adminEvent.path("id").asText())
                .doesNotContain(otherEvent.getId().toString());
    }

    @Test
    void authenticatedAdminCanGetOwnedEvent() throws Exception {
        JsonNode created = createAdminEvent("Owned Event", "2026-09-12", null, true);

        mockMvc.perform(get("/api/events/{id}", created.path("id").asText())
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.path("id").asText()));
    }

    @Test
    void updateChangesOnlyClientEditableFields() throws Exception {
        JsonNode created = createAdminEvent("Original", "2026-09-12", "covers/original.jpg", true);

        mockMvc.perform(put("/api/events/{id}", created.path("id").asText())
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated\",\"eventDate\":\"2026-10-10\",\"coverImageKey\":\"covers/updated.jpg\",\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.path("id").asText()))
                .andExpect(jsonPath("$.publicToken").value(created.path("publicToken").asText()))
                .andExpect(jsonPath("$.name").value("Updated"))
                .andExpect(jsonPath("$.eventDate").value("2026-10-10"))
                .andExpect(jsonPath("$.coverImageKey").value("covers/updated.jpg"))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void unknownEventReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/events/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/events/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Unknown\",\"eventDate\":\"2026-09-12\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void otherUsersEventReturnsNotFoundForGetAndUpdate() throws Exception {
        User otherUser = userRepository.save(new User("other-" + UUID.randomUUID() + "@example.com", passwordEncoder.encode("other-password")));
        Event otherEvent = eventRepository.save(new Event(
                otherUser,
                "Private Event",
                LocalDate.of(2026, 10, 1),
                "other-" + UUID.randomUUID(),
                null,
                true
        ));

        mockMvc.perform(get("/api/events/{id}", otherEvent.getId())
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/events/{id}", otherEvent.getId())
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Attempted Update\",\"eventDate\":\"2026-09-12\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidCreateRequestIsRejected() throws Exception {
        mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }

    private JsonNode createAdminEvent(String name, String eventDate, String coverImageKey, Boolean active) throws Exception {
        String activeField = active == null ? "" : ",\"active\":" + active;
        String coverImageKeyField = coverImageKey == null ? "" : ",\"coverImageKey\":\"" + coverImageKey + "\"";
        String requestBody = "{\"name\":\"" + name + "\",\"eventDate\":\"" + eventDate + "\"" + coverImageKeyField + activeField + "}";

        String response = mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response);
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
