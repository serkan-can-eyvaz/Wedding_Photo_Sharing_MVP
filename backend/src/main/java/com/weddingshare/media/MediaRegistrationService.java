package com.weddingshare.media;

import com.weddingshare.event.Event;
import com.weddingshare.event.PublicEventService;
import com.weddingshare.storage.R2ObjectMetadata;
import com.weddingshare.storage.R2ObjectMetadataService;
import com.weddingshare.upload.UploadRules;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class MediaRegistrationService {

    private final PublicEventService publicEventService;
    private final R2ObjectMetadataService r2ObjectMetadataService;
    private final MediaRepository mediaRepository;

    public MediaRegistrationService(
            PublicEventService publicEventService,
            R2ObjectMetadataService r2ObjectMetadataService,
            MediaRepository mediaRepository
    ) {
        this.publicEventService = publicEventService;
        this.r2ObjectMetadataService = r2ObjectMetadataService;
        this.mediaRepository = mediaRepository;
    }

    public void register(String publicToken, MediaRegistrationRequest request) {
        Event event = publicEventService.findActiveEvent(publicToken);
        validateStorageKey(event, request.storageKey());
        validateOriginalFilename(request.originalFilename());

        if (mediaRepository.existsByStorageKey(request.storageKey())) {
            throw conflict();
        }

        R2ObjectMetadata objectMetadata = r2ObjectMetadataService.getObjectMetadata(request.storageKey());
        UploadRules.UploadRule uploadRule = UploadRules.validate(
                objectMetadata.contentType(),
                objectMetadata.contentLength()
        );
        Media media = new Media(
                event,
                request.storageKey(),
                request.originalFilename(),
                uploadRule.contentType(),
                objectMetadata.contentLength()
        );

        try {
            mediaRepository.saveAndFlush(media);
        } catch (DataIntegrityViolationException exception) {
            throw conflict();
        }
    }

    private void validateStorageKey(Event event, String storageKey) {
        String prefix = "events/" + event.getPublicToken() + "/";
        if (!storageKey.startsWith(prefix)) {
            throw badRequest();
        }

        String fileName = storageKey.substring(prefix.length());
        int extensionSeparator = fileName.lastIndexOf('.');
        if (fileName.indexOf('/') >= 0 || fileName.indexOf('\\') >= 0 || extensionSeparator <= 0
                || extensionSeparator == fileName.length() - 1) {
            throw badRequest();
        }

        String uuidValue = fileName.substring(0, extensionSeparator);
        String extension = fileName.substring(extensionSeparator + 1);
        try {
            if (!UUID.fromString(uuidValue).toString().equals(uuidValue) || !UploadRules.isAllowedExtension(extension)) {
                throw badRequest();
            }
        } catch (IllegalArgumentException exception) {
            throw badRequest();
        }
    }

    private void validateOriginalFilename(String originalFilename) {
        if (!StringUtils.hasText(originalFilename) || originalFilename.length() > 255
                || originalFilename.indexOf('/') >= 0 || originalFilename.indexOf('\\') >= 0
                || originalFilename.chars().anyMatch(Character::isISOControl)) {
            throw badRequest();
        }
    }

    private ResponseStatusException badRequest() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST);
    }

    private ResponseStatusException conflict() {
        return new ResponseStatusException(HttpStatus.CONFLICT);
    }
}
