package com.weddingshare.upload;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/events")
public class PublicUploadController {

    private final PresignedUploadService presignedUploadService;

    public PublicUploadController(PresignedUploadService presignedUploadService) {
        this.presignedUploadService = presignedUploadService;
    }

    @PostMapping("/{token}/uploads/presign")
    public PresignUploadResponse presign(
            @PathVariable String token,
            @Valid @RequestBody PresignUploadRequest request
    ) {
        return presignedUploadService.createPresignedUpload(token, request);
    }
}
