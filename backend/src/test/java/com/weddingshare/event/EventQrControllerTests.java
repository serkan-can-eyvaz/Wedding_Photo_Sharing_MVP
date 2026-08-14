package com.weddingshare.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EventQrControllerTests {

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

    private User owner;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        owner = userRepository.findByEmail(ADMIN_EMAIL).orElseThrow();
    }

    @Test
    void unauthenticatedQrRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/events/{eventId}/qr", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ownedEventReturnsDecodablePngForNormalizedPublicUrlWithoutChangingToken() throws Exception {
        Event event = createEvent(owner, "stable-public-token");
        String originalToken = event.getPublicToken();

        byte[] png = mockMvc.perform(get("/api/events/{eventId}/qr", event.getId())
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        assertThat(png).isNotEmpty();
        assertThat(decodeQr(png)).isEqualTo("https://wedding.example/e/" + originalToken);
        assertThat(eventRepository.findById(event.getId()).orElseThrow().getPublicToken()).isEqualTo(originalToken);
    }

    @Test
    void unknownAndUnownedEventsReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/events/{eventId}/qr", UUID.randomUUID())
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isNotFound());

        User otherOwner = userRepository.save(new User(
                "other-" + UUID.randomUUID() + "@example.com",
                passwordEncoder.encode("other-password")
        ));
        Event unownedEvent = createEvent(otherOwner, "other-public-token");

        mockMvc.perform(get("/api/events/{eventId}/qr", unownedEvent.getId())
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void publicBaseUrlRequiresHttpOrHttpsAndNormalizesTrailingSlashes() {
        assertThat(EventQrService.normalizePublicBaseUrl(" https://example.com/app/// "))
                .isEqualTo("https://example.com/app");

        assertInvalidPublicBaseUrl("");
        assertInvalidPublicBaseUrl("ftp://example.com");
        assertInvalidPublicBaseUrl("example.com");
    }

    private void assertInvalidPublicBaseUrl(String value) {
        assertThatThrownBy(() -> EventQrService.normalizePublicBaseUrl(value))
                .isInstanceOf(IllegalStateException.class);
    }

    private Event createEvent(User eventOwner, String publicToken) {
        return eventRepository.save(new Event(
                eventOwner,
                "QR Event",
                LocalDate.of(2026, 9, 12),
                publicToken,
                null,
                true
        ));
    }

    private String decodeQr(byte[] png) throws Exception {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        assertThat(image).isNotNull();
        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = image.getRGB(0, 0, width, height, null, 0, width);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new RGBLuminanceSource(width, height, pixels)));
        return new MultiFormatReader().decode(bitmap).getText();
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
