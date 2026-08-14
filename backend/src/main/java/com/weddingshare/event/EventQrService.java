package com.weddingshare.event;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.UUID;

@Service
public class EventQrService {

    private static final int QR_SIZE = 512;
    private static final int QR_MARGIN = 4;

    private final EventRepository eventRepository;
    private final String normalizedPublicBaseUrl;

    public EventQrService(
            EventRepository eventRepository,
            @Value("${app.public-base-url}") String publicBaseUrl
    ) {
        this.eventRepository = eventRepository;
        this.normalizedPublicBaseUrl = normalizePublicBaseUrl(publicBaseUrl);
    }

    public byte[] generateForOwnedEvent(UUID ownerId, UUID eventId) {
        Event event = eventRepository.findByIdAndOwnerId(eventId, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return generatePng(guestUrl(event.getPublicToken()));
    }

    static String normalizePublicBaseUrl(String configuredBaseUrl) {
        if (configuredBaseUrl == null || configuredBaseUrl.isBlank()) {
            throw new IllegalStateException("APP_PUBLIC_BASE_URL must be configured");
        }

        String normalized = configuredBaseUrl.trim().replaceFirst("/+$", "");
        try {
            URI uri = new URI(normalized);
            boolean isHttp = "http".equalsIgnoreCase(uri.getScheme());
            boolean isHttps = "https".equalsIgnoreCase(uri.getScheme());
            if ((!isHttp && !isHttps)
                    || uri.getHost() == null
                    || uri.getRawQuery() != null
                    || uri.getRawFragment() != null
                    || uri.getRawUserInfo() != null) {
                throw invalidPublicBaseUrl();
            }
        } catch (URISyntaxException exception) {
            throw invalidPublicBaseUrl();
        }
        return normalized;
    }

    private String guestUrl(String publicToken) {
        return normalizedPublicBaseUrl + "/e/" + publicToken;
    }

    private byte[] generatePng(String payload) {
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(
                    payload,
                    BarcodeFormat.QR_CODE,
                    QR_SIZE,
                    QR_SIZE,
                    Map.of(
                            EncodeHintType.CHARACTER_SET, "UTF-8",
                            EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                            EncodeHintType.MARGIN, QR_MARGIN
                    )
            );
            BufferedImage image = new BufferedImage(QR_SIZE, QR_SIZE, BufferedImage.TYPE_BYTE_BINARY);
            for (int x = 0; x < QR_SIZE; x++) {
                for (int y = 0; y < QR_SIZE; y++) {
                    image.setRGB(x, y, matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            if (!ImageIO.write(image, "PNG", output)) {
                throw new IllegalStateException("PNG writer is unavailable");
            }
            return output.toByteArray();
        } catch (WriterException | IOException exception) {
            throw new IllegalStateException("QR code generation failed", exception);
        }
    }

    private static IllegalStateException invalidPublicBaseUrl() {
        return new IllegalStateException("APP_PUBLIC_BASE_URL must be an absolute http:// or https:// URL");
    }
}
