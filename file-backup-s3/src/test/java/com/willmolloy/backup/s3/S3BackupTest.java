package com.willmolloy.backup.s3;

import static com.google.common.truth.Truth.assertThat;
import static com.willmolloy.backup.util.Md5Helper.md5Base64;
import static com.willmolloy.backup.util.PathHelper.ensureUnixSeparator;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.willmolloy.backup.local.LocalFile;
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
// CHECKSTYLE IGNORE RegexpSinglelineJava FOR NEXT 1 LINES
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.StorageClass;

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

  @BeforeEach
  void setUp() throws IOException {
    fs = Jimfs.newFileSystem(Configuration.forCurrentPlatform());

    Path sourceRoot = fs.getPath("root");
    Files.createDirectory(sourceRoot);
    source = new LocalStorage(sourceRoot);
    destination = new S3Bucket(mockS3Client, "my-bucket", fs.getPath("my/bucket/prefix/backups/"));
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
  void put_whenFile_makesPutRequest() throws IOException {
    // Given
    LocalFile sourceFile = createLocalFile(fs.getPath("A/B/C"));

    // When
    boolean result = sut.put(sourceFile);

    // Then
    assertThat(result).isTrue();
    verify(mockS3Client)
        .putObject(
            PutObjectRequest.builder()
                .bucket("my-bucket")
                .key("my/bucket/prefix/backups/A/B/C")
                .contentMD5(md5Base64(sourceFile.fullPath()))
                .storageClass(StorageClass.DEEP_ARCHIVE)
                .build(),
            sourceFile.fullPath());
  }

  @Test
  void put_whenDirectory_makesPutRequest() throws IOException {
    // Given
    LocalFile sourceDirectory = createLocalDirectory(fs.getPath("A/B/C"));

    // When
    boolean result = sut.put(sourceDirectory);

    // Then
    assertThat(result).isTrue();
    verify(mockS3Client)
        .putObject(
            eq(
                PutObjectRequest.builder()
                    .bucket("my-bucket")
                    .key("my/bucket/prefix/backups/A/B/C/")
                    .storageClass(StorageClass.DEEP_ARCHIVE)
                    .build()),
            // CHECKSTYLE IGNORE RegexpSinglelineJava FOR NEXT 1 LINES
            ArgumentMatchers.<RequestBody>argThat(body -> body.contentLength() == 0));
  }

  @Test
  void put_whenFileNotOnSource_failsGracefully() throws IOException {
    // Given
    LocalFile sourceFile = createLocalFile(fs.getPath("A/B/C"));
    Files.delete(sourceFile.fullPath());

    // When
    boolean result = assertDoesNotThrow(() -> sut.put(sourceFile));

    // Then
    assertThat(result).isTrue();
  }

  @Test
  void put_whenS3Error_failsGracefully() throws IOException {
    // Given
    LocalFile sourceFile = createLocalFile(fs.getPath("A/B/C"));

    when(mockS3Client.putObject(any(PutObjectRequest.class), any(Path.class)))
        .thenThrow(new RuntimeException());

    // When
    boolean result = assertDoesNotThrow(() -> sut.put(sourceFile));

    // Then
    assertThat(result).isFalse();
  }

  @Test
  void delete_whenFile_makesDeleteRequest() {
    // Given
    S3File destFile = createS3Object("X/Y/Z");

    // When
    boolean result = sut.delete(destFile);

    // Then
    assertThat(result).isTrue();
    verify(mockS3Client)
        .deleteObject(
            DeleteObjectRequest.builder()
                .bucket("my-bucket")
                .key("my/bucket/prefix/backups/X/Y/Z")
                .build());
  }

  @Test
  void delete_whenFolder_makesDeleteRequest() {
    // Given
    S3File destFile = createS3Folder("X/Y/Z/");

    // When
    boolean result = sut.delete(destFile);

    // Then
    assertThat(result).isTrue();
    verify(mockS3Client)
        .deleteObject(
            DeleteObjectRequest.builder()
                .bucket("my-bucket")
                .key("my/bucket/prefix/backups/X/Y/Z/")
                .build());
  }

  @Test
  void delete_whenS3Error_failsGracefully() {
    // Given
    S3File destFile = createS3Object("X/Y/Z");
    when(mockS3Client.deleteObject(any(DeleteObjectRequest.class)))
        .thenThrow(new RuntimeException());

    // When
    boolean result = assertDoesNotThrow(() -> sut.delete(destFile));

    // Then
    assertThat(result).isFalse();
  }

  @Test
  void toString_includesSourceAndDest() {
    assertThat(sut.toString())
        .isEqualTo(
            "S3Backup[source=LocalStorage[root], destination=S3Bucket[https://s3.console.aws.amazon.com/s3/buckets/my-bucket?prefix=my/bucket/prefix/backups/]]");
  }

  private LocalFile createLocalFile(Path relativePath) throws IOException {
    Path path = source.root().resolve(relativePath);
    Path parent = path.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    Files.createFile(path);
    return LocalFile.fromPath(source, path);
  }

  private LocalFile createLocalDirectory(Path relativePath) throws IOException {
    Path path = source.root().resolve(relativePath);
    Files.createDirectories(path);
    return LocalFile.fromPath(source, path);
  }

  private S3File createS3Object(String key) {
    S3Object s3Object =
        S3Object.builder().key(ensureUnixSeparator(destination.prefix().resolve(key))).build();
    return S3File.fromS3Object(destination, s3Object);
  }

  private S3File createS3Folder(String key) {
    S3Object s3Object =
        S3Object.builder()
            .key(ensureUnixSeparator(destination.prefix().resolve(key)) + "/")
            .build();
    return S3File.fromS3Object(destination, s3Object);
  }
}
