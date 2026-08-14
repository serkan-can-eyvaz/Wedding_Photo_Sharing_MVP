package com.weddingshare;

import com.weddingshare.storage.R2ObjectMetadata;
import com.weddingshare.storage.R2ObjectMetadataService;
import com.weddingshare.storage.R2Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class R2ObjectMetadataServiceTests {

    @Mock
    private S3Client r2S3Client;

    @Mock
    private R2Properties r2Properties;

    @Test
    void metadataLookupUsesHeadObjectWithoutDownloadingTheObjectBody() {
        when(r2Properties.bucket()).thenReturn("test-r2-bucket");
        when(r2S3Client.headObject(any(HeadObjectRequest.class))).thenReturn(HeadObjectResponse.builder()
                .contentType("image/jpeg")
                .contentLength(1024L)
                .build());
        R2ObjectMetadataService metadataService = new R2ObjectMetadataService(r2S3Client, r2Properties);

        R2ObjectMetadata metadata = metadataService.getObjectMetadata("events/event-token/object.jpg");

        ArgumentCaptor<HeadObjectRequest> requestCaptor = ArgumentCaptor.forClass(HeadObjectRequest.class);
        verify(r2S3Client).headObject(requestCaptor.capture());
        verifyNoMoreInteractions(r2S3Client);
        assertThat(requestCaptor.getValue().bucket()).isEqualTo("test-r2-bucket");
        assertThat(requestCaptor.getValue().key()).isEqualTo("events/event-token/object.jpg");
        assertThat(metadata.contentType()).isEqualTo("image/jpeg");
        assertThat(metadata.contentLength()).isEqualTo(1024L);
    }

    @Test
    void missingObjectAndUpstreamFailureAreMappedWithoutR2Details() {
        when(r2Properties.bucket()).thenReturn("test-r2-bucket");
        R2ObjectMetadataService metadataService = new R2ObjectMetadataService(r2S3Client, r2Properties);
        when(r2S3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(404).message("missing object").build());

        assertThatThrownBy(() -> metadataService.getObjectMetadata("events/event-token/missing.jpg"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        when(r2S3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(500).message("internal R2 detail").build());

        assertThatThrownBy(() -> metadataService.getObjectMetadata("events/event-token/failing.jpg"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY));
    }
}
