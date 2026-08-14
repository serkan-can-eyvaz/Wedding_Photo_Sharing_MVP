package com.weddingshare.media;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/events")
public class PublicMediaController {

    private final MediaRegistrationService mediaRegistrationService;

    public PublicMediaController(MediaRegistrationService mediaRegistrationService) {
        this.mediaRegistrationService = mediaRegistrationService;
    }

    @PostMapping("/{token}/media")
    public ResponseEntity<Void> register(
            @PathVariable String token,
            @Valid @RequestBody MediaRegistrationRequest request
    ) {
        mediaRegistrationService.register(token, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
