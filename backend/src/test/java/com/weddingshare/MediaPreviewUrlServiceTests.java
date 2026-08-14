package com.weddingshare;

import com.weddingshare.media.Media;
import com.weddingshare.storage.MediaPreviewUrlService;
import com.weddingshare.storage.R2Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URL;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediaPreviewUrlServiceTests {

    @Mock
    private S3Presigner r2S3Presigner;

    @Mock
    private R2Properties r2Properties;

    @Mock
    private PresignedGetObjectRequest presignedRequest;

    @Test
    void imagePreviewUsesFifteenMinutePresignedGetWithoutNetworkAccess() throws Exception {
        MediaPreviewUrlService service = new MediaPreviewUrlService(r2S3Presigner, r2Properties);
        Media image = new Media(null, "events/example/image.jpg", "image.jpg", "image/jpeg", 1024);
        when(r2Properties.bucket()).thenReturn("test-r2-bucket");
        when(r2S3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presignedRequest);
        when(presignedRequest.url()).thenReturn(new URL("https://example.invalid/image?X-Amz-Expires=900"));

        String previewUrl = service.createImagePreviewUrl(image);

        ArgumentCaptor<GetObjectPresignRequest> requestCaptor = ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        verify(r2S3Presigner).presignGetObject(requestCaptor.capture());
        assertThat(requestCaptor.getValue().signatureDuration()).isEqualTo(Duration.ofMinutes(15));
        assertThat(requestCaptor.getValue().getObjectRequest().bucket()).isEqualTo("test-r2-bucket");
        assertThat(requestCaptor.getValue().getObjectRequest().key()).isEqualTo("events/example/image.jpg");
        assertThat(previewUrl).contains("X-Amz-Expires=900");
    }

    @Test
    void videoDoesNotCreatePreviewUrl() {
        MediaPreviewUrlService service = new MediaPreviewUrlService(r2S3Presigner, r2Properties);
        Media video = new Media(null, "events/example/video.mp4", "video.mp4", "video/mp4", 1024);

        assertThat(service.createImagePreviewUrl(video)).isNull();
        verify(r2S3Presigner, never()).presignGetObject(any(GetObjectPresignRequest.class));
    }
}
