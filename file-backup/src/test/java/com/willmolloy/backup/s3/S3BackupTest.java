package com.willmolloy.backup.s3;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.github.javafaker.Faker;
import com.willmolloy.backup.local.LocalStorage;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * S3BackupTest.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
@ExtendWith(MockitoExtension.class)
class S3BackupTest {

  @Mock private S3Client mockS3Client;
  private S3Backup sut;

  private static final String ROOT_DIR = "root";
  private static final String BUCKET_NAME = "test-bucket";
  private static final String BUCKET_PREFIX = "testing/backup/";

  @BeforeEach
  void setUp() {
    sut =
        new S3Backup(
            mockS3Client,
            new LocalStorage(Path.of(ROOT_DIR)),
            new S3Bucket(mockS3Client, BUCKET_NAME, BUCKET_PREFIX));
  }

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(mockS3Client);
  }

  @Test
  void copy_makesPutRequest() {
    // Given
    String fileName = fakeFileName();

    // When
    sut.copy(fileName);

    // Then
    verify(mockS3Client)
        .putObject(
            PutObjectRequest.builder().bucket(BUCKET_NAME).key(BUCKET_PREFIX + fileName).build(),
            Path.of(ROOT_DIR + "/" + fileName));
  }

  @Test
  void copy_failsGracefully() {
    // Given
    when(mockS3Client.putObject(any(PutObjectRequest.class), any(Path.class)))
        .thenThrow(new RuntimeException());

    // When
    assertDoesNotThrow(() -> sut.copy(fakeFileName()));
  }

  @Test
  void update_makesPutRequest() {
    // Given
    String fileName = fakeFileName();

    // When
    sut.update(fileName);

    // Then
    verify(mockS3Client)
        .putObject(
            PutObjectRequest.builder().bucket(BUCKET_NAME).key(BUCKET_PREFIX + fileName).build(),
            Path.of(ROOT_DIR + "/" + fileName));
  }

  @Test
  void update_failsGracefully() {
    // Given
    when(mockS3Client.putObject(any(PutObjectRequest.class), any(Path.class)))
        .thenThrow(new RuntimeException());

    // When
    assertDoesNotThrow(() -> sut.update(fakeFileName()));
  }

  @Test
  void delete_makesDeleteRequest() {
    // Given
    String fileName = fakeFileName();

    // When
    sut.delete(fileName);

    // Then
    verify(mockS3Client)
        .deleteObject(
            DeleteObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(BUCKET_PREFIX + fileName)
                .build());
  }

  @Test
  void delete_failsGracefully() {
    // Given
    when(mockS3Client.deleteObject(any(DeleteObjectRequest.class)))
        .thenThrow(new RuntimeException());

    // When
    assertDoesNotThrow(() -> sut.delete(fakeFileName()));
  }

  @Test
  void toString_includesSourceAndDest() {
    assertThat(sut.toString())
        .isEqualTo(
            "S3Backup[source=LocalStorage[root], destination=S3Bucket[https://s3.console.aws.amazon.com/s3/buckets/test-bucket?prefix=testing/backup/]]");
  }

  private String fakeFileName() {
    return new Faker().file().fileName(null, null, null, "/");
  }
}
