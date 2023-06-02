package com.willmolloy.backup.s3;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.github.javafaker.Faker;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * S3FileTest.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
@SuppressFBWarnings("UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
@ExtendWith(MockitoExtension.class)
class S3FileTest {

  private static final Faker FAKER = new Faker();

  @Mock private S3Client mockS3Client;
  private S3Bucket bucket;

  @BeforeEach
  void setUp() {
    bucket = new S3Bucket(mockS3Client, "my-bucket", Path.of("prefix"));
  }

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(mockS3Client);
  }

  @Test
  void uri_whenObject_returnsObjectUri() {
    // Given
    S3Object s3Object = S3Object.builder().key("prefix/A/B/C").build();
    S3File file = S3File.fromS3Object(bucket, s3Object);

    // Then
    assertThat(file.uri())
        .isEqualTo("https://s3.console.aws.amazon.com/s3/object/my-bucket?prefix=prefix/A/B/C");
  }

  @Test
  void uri_whenFolder_returnsFolderUri() {
    // Given
    S3Object s3Object = S3Object.builder().key("prefix/A/B/C/").build();
    S3File file = S3File.fromS3Object(bucket, s3Object);

    // Then
    assertThat(file.uri())
        .isEqualTo("https://s3.console.aws.amazon.com/s3/buckets/my-bucket?prefix=prefix/A/B/C/");
  }

  @Test
  void relativePath_returnsRelativizedPath() {
    // Given
    S3Object s3Object = S3Object.builder().key("prefix/A/B/C").build();
    S3File file = S3File.fromS3Object(bucket, s3Object);

    // Then
    assertThat(file.relativePath()).isEqualTo(Path.of("A/B/C"));
  }

  @Test
  void size_returnsS3ObjectSize() {
    // Given
    long randomLong = FAKER.number().numberBetween(1, 10);
    S3Object s3Object = S3Object.builder().key("prefix/A").size(randomLong).build();
    S3File file = S3File.fromS3Object(bucket, s3Object);

    // When
    long result = file.size();

    // Then
    assertThat(result).isEqualTo(randomLong);
  }

  @Test
  void isDirectory_whenObject_false() {
    // Given
    S3Object s3Object = S3Object.builder().key("prefix/A/B/C").build();
    S3File file = S3File.fromS3Object(bucket, s3Object);

    // Then
    assertThat(file.isDirectory()).isFalse();
  }

  @Test
  void isDirectory_whenFolder_true() {
    // Given
    S3Object s3Object = S3Object.builder().key("prefix/A/B/C/").build();
    S3File file = S3File.fromS3Object(bucket, s3Object);

    // Then
    assertThat(file.isDirectory()).isTrue();
  }

  @Test
  void toString_whenObject_includesObjectUri() {
    // Given
    S3Object s3Object = S3Object.builder().key("prefix/A/B/C").build();
    S3File file = S3File.fromS3Object(bucket, s3Object);

    // Then
    assertThat(file.toString())
        .isEqualTo(
            "S3File[https://s3.console.aws.amazon.com/s3/object/my-bucket?prefix=prefix/A/B/C]");
  }

  @Test
  void toString_whenFolder_includesFolderUri() {
    // Given
    S3Object s3Object = S3Object.builder().key("prefix/A/B/C/").build();
    S3File file = S3File.fromS3Object(bucket, s3Object);

    // Then
    assertThat(file.toString())
        .isEqualTo(
            "S3File[https://s3.console.aws.amazon.com/s3/buckets/my-bucket?prefix=prefix/A/B/C/]");
  }

  @Test
  void constructor_requiresObjectKeyBelowBucketPrefix() {
    // Given
    S3Object s3Object = S3Object.builder().key("A").build();

    // Then
    IllegalArgumentException thrown =
        assertThrows(IllegalArgumentException.class, () -> S3File.fromS3Object(bucket, s3Object));
    assertThat(thrown)
        .hasMessageThat()
        .isEqualTo("Requires object key [A] to be under bucket prefix [prefix]");
  }

  @Test
  void directoryFiller() {
    // Given
    S3File directoryFiller = S3File.directoryFiller(bucket, "A/B/C");

    // Then
    Path relativePath = bucket.prefix().getFileSystem().getPath("A/B/C");
    assertThat(directoryFiller.uri()).isEqualTo(bucket.folderUri(relativePath));
    assertThat(directoryFiller.relativePath()).isEqualTo(relativePath);
    assertThat(directoryFiller.isDirectory()).isEqualTo(true);
    assertThat(directoryFiller.size()).isEqualTo(0);
  }
}
