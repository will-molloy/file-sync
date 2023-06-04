package com.willmolloy.backup.s3;

import static com.google.common.truth.Truth.assertThat;
import static com.willmolloy.backup.util.Md5Helper.md5Base64;
import static com.willmolloy.backup.util.PathHelper.ensureUnixSeparator;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.Range;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.willmolloy.backup.local.LocalFile;
import com.willmolloy.backup.local.LocalStorage;
import com.willmolloy.backup.statistics.LoggingBackupObserver;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.StorageClass;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;

/**
 * S3BackupTest.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
@SuppressFBWarnings("UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
@ExtendWith(MockitoExtension.class)
class S3BackupTest {

  @Mock private S3Client mockS3Client;
  @Mock private S3Waiter mockS3Waiter;
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
    sut =
        new S3Backup(
            mockS3Client, mockS3Waiter, source, destination, List.of(new LoggingBackupObserver()));
  }

  @AfterEach
  void tearDown() throws IOException {
    fs.close();
    verifyNoMoreInteractions(mockS3Client);
    verifyNoMoreInteractions(mockS3Waiter);
  }

  @Test
  void put_whenFile_makesPutObjectRequest() throws IOException {
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
    verify(mockS3Waiter)
        .waitUntilObjectExists(
            HeadObjectRequest.builder()
                .bucket("my-bucket")
                .key("my/bucket/prefix/backups/A/B/C")
                .build());
  }

  @Test
  void put_whenDirectory_makesPutObjectRequest() throws IOException {
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
            argThat(emptyRequestBody()));
    verify(mockS3Waiter)
        .waitUntilObjectExists(
            HeadObjectRequest.builder()
                .bucket("my-bucket")
                .key("my/bucket/prefix/backups/A/B/C/")
                .build());
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
  void delete_whenFile_makesDeleteObjectRequest() {
    // Given
    S3File destFile = createS3Object("X/Y/Z");

    // When
    boolean result = sut.delete(destination.scan().subtree(destFile));

    // Then
    assertThat(result).isTrue();
    verify(mockS3Client)
        .deleteObject(
            DeleteObjectRequest.builder()
                .bucket("my-bucket")
                .key("my/bucket/prefix/backups/X/Y/Z")
                .build());
    verify(mockS3Waiter)
        .waitUntilObjectNotExists(
            HeadObjectRequest.builder()
                .bucket("my-bucket")
                .key("my/bucket/prefix/backups/X/Y/Z")
                .build());
  }

  @Test
  void delete_whenFolder_makesDeleteObjectsRequest() {
    // Given
    S3File destFolder = createS3Folder("folder/", List.of("A", "B", "C", "D/E", "D/F/G"));

    // When
    boolean result = sut.delete(destination.scan().subtree(destFolder));

    // Then
    assertThat(result).isTrue();
    verify(mockS3Client)
        .deleteObjects(
            DeleteObjectsRequest.builder()
                .bucket("my-bucket")
                .delete(
                    Delete.builder()
                        .objects(
                            ObjectIdentifier.builder()
                                .key("my/bucket/prefix/backups/folder/A")
                                .build(),
                            ObjectIdentifier.builder()
                                .key("my/bucket/prefix/backups/folder/B")
                                .build(),
                            ObjectIdentifier.builder()
                                .key("my/bucket/prefix/backups/folder/C")
                                .build(),
                            ObjectIdentifier.builder()
                                .key("my/bucket/prefix/backups/folder/D/E")
                                .build(),
                            ObjectIdentifier.builder()
                                .key("my/bucket/prefix/backups/folder/D/F/G")
                                .build())
                        .build())
                .build());
  }

  @Test
  void delete_whenFolder_makesDeleteObjectsRequestsInChunksOf1000Keys() {
    // Given
    S3File destFolder =
        createS3Folder(
            "folder/", IntStream.rangeClosed(0, 2010).mapToObj(String::valueOf).toList());

    // When
    boolean result = sut.delete(destination.scan().subtree(destFolder));

    // Then
    assertThat(result).isTrue();
    for (Range<Integer> range :
        List.of(Range.closed(0, 999), Range.closed(1000, 1999), Range.closed(2000, 2010))) {
      verify(mockS3Client)
          .deleteObjects(
              DeleteObjectsRequest.builder()
                  .bucket("my-bucket")
                  .delete(
                      Delete.builder()
                          .objects(
                              IntStream.rangeClosed(range.lowerEndpoint(), range.upperEndpoint())
                                  .mapToObj(
                                      i ->
                                          ObjectIdentifier.builder()
                                              .key("my/bucket/prefix/backups/folder/" + i)
                                              .build())
                                  .toList())
                          .build())
                  .build());
    }
  }

  @Test
  void delete_whenS3Error_failsGracefully() {
    // Given
    S3File destFile = createS3Object("X/Y/Z");
    when(mockS3Client.deleteObject(any(DeleteObjectRequest.class)))
        .thenThrow(new RuntimeException());

    // When
    boolean result = assertDoesNotThrow(() -> sut.delete(destination.scan().subtree(destFile)));

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
    S3Object object =
        S3Object.builder().key(ensureUnixSeparator(destination.prefix().resolve(key))).build();
    mockListResponse(List.of(object));
    return S3File.fromS3Object(destination, object);
  }

  private S3File createS3Folder(String key, List<String> childObjectKeys) {
    S3Object folder =
        S3Object.builder()
            .key(ensureUnixSeparator(destination.prefix().resolve(key)) + "/")
            .build();
    List<S3Object> objects =
        childObjectKeys.stream()
            .map(objectKey -> S3Object.builder().key(folder.key() + objectKey).build())
            .toList();
    mockListResponse(objects);
    return S3File.fromS3Object(destination, folder);
  }

  private void mockListResponse(List<S3Object> objects) {
    ListObjectsV2Response page = ListObjectsV2Response.builder().contents(objects).build();
    ListObjectsV2Iterable response = mock(ListObjectsV2Iterable.class);
    when(response.iterator()).thenReturn(List.of(page).iterator());
    when(mockS3Client.listObjectsV2Paginator(
            ListObjectsV2Request.builder()
                .bucket("my-bucket")
                .prefix("my/bucket/prefix/backups/")
                .build()))
        .thenReturn(response);
  }

  private ArgumentMatcher<RequestBody> emptyRequestBody() {
    return actual ->
        actual.optionalContentLength().map(contentLength -> contentLength == 0).orElse(false);
  }
}
