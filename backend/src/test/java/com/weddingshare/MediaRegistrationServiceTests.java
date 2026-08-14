package com.weddingshare;

import com.weddingshare.event.Event;
import com.weddingshare.event.PublicEventService;
import com.weddingshare.media.MediaRegistrationRequest;
import com.weddingshare.media.MediaRegistrationService;
import com.weddingshare.media.MediaRepository;
import com.weddingshare.storage.R2ObjectMetadata;
import com.weddingshare.storage.R2ObjectMetadataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediaRegistrationServiceTests {

    @Mock
    private PublicEventService publicEventService;

    @Mock
    private R2ObjectMetadataService r2ObjectMetadataService;

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private Event event;

    @Test
    void databaseUniqueViolationDuringConcurrentRegistrationReturnsConflict() {
        String token = "concurrent-registration-token";
        String storageKey = "events/" + token + "/" + UUID.randomUUID() + ".jpg";
        when(event.getPublicToken()).thenReturn(token);
        when(publicEventService.findActiveEvent(token)).thenReturn(event);
        when(mediaRepository.existsByStorageKey(storageKey)).thenReturn(false);
        when(r2ObjectMetadataService.getObjectMetadata(storageKey)).thenReturn(new R2ObjectMetadata("image/jpeg", 1024));
        when(mediaRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate key"));
        MediaRegistrationService registrationService = new MediaRegistrationService(
                publicEventService,
                r2ObjectMetadataService,
                mediaRepository
        );

        assertThatThrownBy(() -> registrationService.register(token, new MediaRegistrationRequest(storageKey, "photo.jpg")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }
}
