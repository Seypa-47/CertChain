package com.certchain.artifact;

import java.io.ByteArrayInputStream;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class S3ArtifactStorageTests {
    private final S3Client client = mock(S3Client.class);
    private final S3ArtifactStorage storage = new S3ArtifactStorage(client, "private-certificates");
    private final String key = "certificates/" + UUID.randomUUID() + "/certificate.pdf";

    @Test
    void requiresHttpsStorageOriginAndCredentials() {
        assertThrows(IllegalArgumentException.class,
            () -> new S3ArtifactStorage("http://storage.example", "us-east-2", "private", "id", "secret"));
        assertThrows(IllegalArgumentException.class,
            () -> new S3ArtifactStorage("https://storage.example/other-path", "us-east-2", "private", "id", "secret"));
        assertThrows(IllegalArgumentException.class,
            () -> new S3ArtifactStorage("https://storage.example", "us-east-2", "private", "", "secret"));
    }

    @Test
    void restrictsKeysAndSizesBeforeContactingStorage() {
        assertThrows(IllegalArgumentException.class, () -> storage.write("../secret", new byte[] {1}));
        assertThrows(IllegalArgumentException.class, () -> storage.read("certificates/../../secret"));
        assertThrows(IllegalArgumentException.class, () -> storage.exists("/absolute/path"));
        assertThrows(IllegalArgumentException.class, () -> storage.write(key, new byte[0]));
        assertThrows(IllegalArgumentException.class, () -> storage.write(key, new byte[10 * 1024 * 1024 + 1]));
        verifyNoInteractions(client);
    }

    @Test
    void storesPrivateObjectAndReadsItWithinLimit() {
        byte[] pdf = {1, 2, 3};
        storage.write(key, pdf);
        verify(client).putObject(argThat((PutObjectRequest request) ->
            request.bucket().equals("private-certificates") && request.key().equals(key)
                && request.contentType().equals("application/pdf")), any(RequestBody.class));

        when(client.headObject(any(HeadObjectRequest.class)))
            .thenReturn(HeadObjectResponse.builder().contentLength(3L).build());
        when(client.getObject(any(GetObjectRequest.class))).thenReturn(new ResponseInputStream<>(
            GetObjectResponse.builder().build(), new ByteArrayInputStream(pdf)));
        assertTrue(storage.exists(key));
        assertArrayEquals(pdf, storage.read(key));
    }

    @Test
    void doesNotReadOversizedOrMissingObjects() {
        when(client.headObject(any(HeadObjectRequest.class)))
            .thenReturn(HeadObjectResponse.builder().contentLength(10L * 1024 * 1024 + 1).build())
            .thenThrow(S3Exception.builder().statusCode(404).build());
        assertThrows(IllegalStateException.class, () -> storage.read(key));
        verify(client, never()).getObject(any(GetObjectRequest.class));
        assertFalse(storage.exists(key));
    }
}
