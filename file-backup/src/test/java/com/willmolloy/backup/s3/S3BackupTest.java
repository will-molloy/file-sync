package com.willmolloy.backup.s3;

import static com.google.common.truth.Truth.assertThat;
import static com.willmolloy.backup.util.Md5Helper.md5Base64;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.github.javafaker.Faker;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.willmolloy.backup.local.LocalStorage;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
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
@SuppressFBWarnings("UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
@ExtendWith(MockitoExtension.class)
class S3BackupTest {

  @Mock private S3Client mockS3Client;
  private LocalStorage source;
  private S3Bucket destination;
  private S3Backup sut;

  private FileSystem fs;
  private Path sourceRoot;
  private static final String DEST_BUCKET = "test-bucket";
  private static final String DEST_BUCKET_PREFIX = "testing/backup/";

  @BeforeEach
  void setUp() throws IOException {
    fs = Jimfs.newFileSystem(Configuration.forCurrentPlatform());

    sourceRoot = fs.getPath("root");
    Files.createDirectory(sourceRoot);

    source = new LocalStorage(sourceRoot);
    destination = new S3Bucket(mockS3Client, DEST_BUCKET, DEST_BUCKET_PREFIX);
    sut = new S3Backup(mockS3Client, source, destination);
  }

  @AfterEach
  void tearDown() throws IOException {
    fs.close();
    verifyNoMoreInteractions(mockS3Client);
  }

  @Test
  void source_returnsSource() {
    assertThat(sut.source()).isSameInstanceAs(source);
  }

  @Test
  void destination_returnsDestination() {
    assertThat(sut.destination()).isSameInstanceAs(destination);
  }

  @Test
  void put_makesPutRequest() throws IOException {
    // Given
    String key = fakeFileName();
    Path sourcePath = createSourcePath(key);

    // When
    boolean result = sut.put(key);

    // Then
    assertThat(result).isTrue();
    verify(mockS3Client)
        .putObject(
            PutObjectRequest.builder()
                .bucket(DEST_BUCKET)
                .key(DEST_BUCKET_PREFIX + key)
                .contentMD5(md5Base64(sourcePath))
                .build(),
            sourcePath);
  }

  @Test
  void put_failsGracefully() {
    // When
    boolean result = assertDoesNotThrow(() -> sut.put(fakeFileName()));

    // Then
    assertThat(result).isFalse();
  }

  @Test
  void delete_makesDeleteRequest() {
    // Given
    String key = fakeFileName();

    // When
    boolean result = sut.delete(key);

    // Then
    assertThat(result).isTrue();
    verify(mockS3Client)
        .deleteObject(
            DeleteObjectRequest.builder()
                .bucket(DEST_BUCKET)
                .key(DEST_BUCKET_PREFIX + key)
                .build());
  }

  @Test
  void delete_failsGracefully() {
    // Given
    when(mockS3Client.deleteObject(any(DeleteObjectRequest.class)))
        .thenThrow(new RuntimeException());

    // When
    boolean result = assertDoesNotThrow(() -> sut.delete(fakeFileName()));

    // Then
    assertThat(result).isFalse();
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

  private Path createSourcePath(String key) throws IOException {
    Path sourcePath = sourceRoot.resolve(key);
    Path parent = sourcePath.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    return Files.createFile(sourcePath);
  }
}
