package com.certchain.artifact;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(properties = {
    "app.storage.type=s3",
    "app.storage.s3.endpoint=https://storage.example.test",
    "app.storage.s3.region=ap-southeast-1",
    "app.storage.s3.bucket=certchain-pdfs",
    "app.storage.s3.access-key-id=test-id",
    "app.storage.s3.secret-access-key=test-secret"
})
class S3ArtifactStorageContextTests {
    @Autowired
    private ArtifactStorage storage;

    @Test
    void springCreatesS3StorageWithConfiguredConstructor() {
        assertInstanceOf(S3ArtifactStorage.class, storage);
    }
}
