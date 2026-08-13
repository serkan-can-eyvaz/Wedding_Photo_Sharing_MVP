package com.weddingshare;

import com.weddingshare.event.Event;
import com.weddingshare.event.EventRepository;
import com.weddingshare.user.User;
import com.weddingshare.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PublicEventControllerTests {

    private static final String ADMIN_EMAIL = "admin@example.com";

    @Autowired
    private MockMvc mockMvc;

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
    void activeEventIsAvailableWithoutAuthenticationAndExposesOnlyPublicFields() throws Exception {
        eventRepository.save(new Event(
                owner,
                "Ayşe & Mehmet",
                LocalDate.of(2026, 9, 12),
                "active-public-token",
                "covers/ayse-mehmet.jpg",
                true
        ));

        mockMvc.perform(get("/api/public/events/{token}", "active-public-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ayşe & Mehmet"))
                .andExpect(jsonPath("$.eventDate").value("2026-09-12"))
                .andExpect(jsonPath("$.coverImageKey").value("covers/ayse-mehmet.jpg"))
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.owner").doesNotExist())
                .andExpect(jsonPath("$.publicToken").doesNotExist())
                .andExpect(jsonPath("$.createdAt").doesNotExist())
                .andExpect(jsonPath("$.active").doesNotExist());
    }

    @Test
    void unknownTokenReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/public/events/{token}", "unknown-token"))
                .andExpect(status().isNotFound());
    }

    @Test
    void inactiveEventReturnsNotFound() throws Exception {
        eventRepository.save(new Event(
                owner,
                "Inactive Event",
                LocalDate.of(2026, 9, 12),
                "inactive-public-token",
                null,
                false
        ));

        mockMvc.perform(get("/api/public/events/{token}", "inactive-public-token"))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminEndpointsRemainProtectedWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/events"))
                .andExpect(status().isUnauthorized());
    }
}
