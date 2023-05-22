package com.willmolloy.backup.s3;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.github.javafaker.Faker;
import com.willmolloy.backup.File;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
    S3File file = new S3File(bucket, s3Object);

    // Then
    assertThat(file.uri())
        .isEqualTo("https://s3.console.aws.amazon.com/s3/object/my-bucket?prefix=prefix/A/B/C");
  }

  @Test
  void uri_whenFolder_returnsFolderUri() {
    // Given
    S3Object s3Object = S3Object.builder().key("prefix/A/B/C/").build();
    S3File file = new S3File(bucket, s3Object);

    // Then
    assertThat(file.uri())
        .isEqualTo("https://s3.console.aws.amazon.com/s3/buckets/my-bucket?prefix=prefix/A/B/C/");
  }

  @Test
  void relativePath_returnsRelativizedPath() {
    // Given
    S3Object s3Object = S3Object.builder().key("prefix/A/B/C").build();
    S3File file = new S3File(bucket, s3Object);

    // Then
    assertThat(file.relativePath()).isEqualTo(Path.of("A/B/C"));
  }

  @Test
  void size_returnsS3ObjectSize() {
    // Given
    long randomLong = FAKER.random().nextLong();
    S3Object s3Object = S3Object.builder().key("prefix/A").size(randomLong).build();
    S3File file = new S3File(bucket, s3Object);

    // When
    long result = file.size();

    // Then
    assertThat(result).isEqualTo(randomLong);
  }

  @Test
  void isDirectory_whenObject_false() {
    // Given
    S3Object s3Object = S3Object.builder().key("prefix/A/B/C").build();
    S3File file = new S3File(bucket, s3Object);

    // Then
    assertThat(file.isDirectory()).isFalse();
  }

  @Test
  void isDirectory_whenFolder_true() {
    // Given
    S3Object s3Object = S3Object.builder().key("prefix/A/B/C/").build();
    S3File file = new S3File(bucket, s3Object);

    // Then
    assertThat(file.isDirectory()).isTrue();
  }

  @ParameterizedTest
  @MethodSource
  void same_onlyTrueIfSizeEqual(long thisSize, long otherSize, boolean expected) {
    // Given
    S3Object s3Object = S3Object.builder().key("prefix/A").size(thisSize).build();
    S3File thisFile = spy(new S3File(bucket, s3Object));

    File otherFile = mock(File.class);
    when(otherFile.size()).thenReturn(otherSize);

    // Then
    assertThat(thisFile.same(otherFile)).isEqualTo(expected);
  }

  static Stream<Arguments> same_onlyTrueIfSizeEqual() {
    long randomLong = FAKER.random().nextLong();
    return Stream.of(
        Arguments.of(randomLong, randomLong, true),
        Arguments.of(randomLong, randomLong + 1, false),
        Arguments.of(randomLong, randomLong - 1, false));
  }

  @Test
  void toString_whenObject_includesObjectUri() {
    // Given
    S3Object s3Object = S3Object.builder().key("prefix/A/B/C").build();
    S3File file = new S3File(bucket, s3Object);

    // Then
    assertThat(file.toString())
        .isEqualTo(
            "S3File[https://s3.console.aws.amazon.com/s3/object/my-bucket?prefix=prefix/A/B/C]");
  }

  @Test
  void toString_whenFolder_includesFolderUri() {
    // Given
    S3Object s3Object = S3Object.builder().key("prefix/A/B/C/").build();
    S3File file = new S3File(bucket, s3Object);

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
        assertThrows(IllegalArgumentException.class, () -> new S3File(bucket, s3Object));
    assertThat(thrown)
        .hasMessageThat()
        .isEqualTo("Requires object key [A] to be under bucket prefix [prefix]");
  }
}
