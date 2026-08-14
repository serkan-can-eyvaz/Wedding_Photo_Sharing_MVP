package com.weddingshare;

import com.weddingshare.storage.R2MediaDownloadService;
import com.weddingshare.storage.R2Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class R2MediaDownloadServiceTests {

    @Mock
    private S3Client r2S3Client;

    @Mock
    private R2Properties r2Properties;

    @Mock
    private ResponseInputStream<GetObjectResponse> responseStream;

    @Test
    void opensObjectAsSdkStreamWithoutExternalHttpClient() {
        R2MediaDownloadService service = new R2MediaDownloadService(r2S3Client, r2Properties);
        when(r2Properties.bucket()).thenReturn("test-r2-bucket");
        when(r2S3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseStream);

        InputStream stream = service.openObjectStream("events/example/image.jpg");

        ArgumentCaptor<GetObjectRequest> requestCaptor = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(r2S3Client).getObject(requestCaptor.capture());
        assertThat(stream).isSameAs(responseStream);
        assertThat(requestCaptor.getValue().bucket()).isEqualTo("test-r2-bucket");
        assertThat(requestCaptor.getValue().key()).isEqualTo("events/example/image.jpg");
    }
}
