package com.willmolloy.backup.s3;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javafaker.Faker;
import com.willmolloy.backup.Backup;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * S3FileTest.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
class S3FileTest {

  private static final Faker FAKER = new Faker();

  @Test
  void size_returnsS3ObjectSize() {
    // Given
    long randomLong = FAKER.random().nextLong();
    S3Object s3Object = S3Object.builder().size(randomLong).build();
    S3File file = new S3File(s3Object);

    // When
    long result = file.size();

    // Then
    assertThat(result).isEqualTo(randomLong);
  }

  @Test
  void size_whenNoSizeAttribute_failsGracefully() {
    // Given
    S3Object s3Object = S3Object.builder().build();
    S3File file = new S3File(s3Object);

    // When
    long result = assertDoesNotThrow(() -> file.size());

    // Then
    assertThat(result).isEqualTo(0);
  }

  @Test
  void lastModified_notSupported() {
    // Given
    S3Object s3Object = S3Object.builder().build();
    S3File file = new S3File(s3Object);

    // Then
    assertThrows(UnsupportedOperationException.class, () -> file.lastModified());
  }

  @Test
  void etag_returnsS3ObjectETag() {
    // Given
    String randomString = FAKER.code().asin();
    S3Object s3Object = S3Object.builder().eTag(randomString).build();
    S3File file = new S3File(s3Object);

    // When
    String result = file.etag();

    // Then
    assertThat(result).isEqualTo(randomString);
  }

  @Test
  void etag_whenNoETagAttribute_failsGracefully() {
    // Given
    S3Object s3Object = S3Object.builder().build();
    S3File file = new S3File(s3Object);

    // When
    String result = assertDoesNotThrow(() -> file.etag());

    // Then
    assertThat(result).isEmpty();
  }

  @ParameterizedTest
  @MethodSource
  void equal_onlyTrueIfSizeAndETagEqual(
      long thisSize, String thisETag, long otherSize, String otherETag, boolean expected) {
    // Given
    S3File thisFile = spy(new S3File(S3Object.builder().size(thisSize).eTag(thisETag).build()));

    Backup.File otherFile = mock(Backup.File.class);
    when(otherFile.size()).thenReturn(otherSize);
    when(otherFile.etag()).thenReturn(otherETag);

    // Then
    assertThat(thisFile.equal(otherFile)).isEqualTo(expected);
    boolean etagCalled = thisSize == otherSize;
    verify(thisFile, etagCalled ? times(1) : never()).etag();
    verify(otherFile, etagCalled ? times(1) : never()).etag();
  }

  static Stream<Arguments> equal_onlyTrueIfSizeAndETagEqual() {
    long randomLong = FAKER.random().nextLong();
    String randomString = FAKER.code().asin();
    return Stream.of(
        Arguments.of(randomLong, randomString, randomLong, randomString, true),
        Arguments.of(randomLong, randomString, randomLong + 1, randomString, false),
        Arguments.of(randomLong, randomString, randomLong, randomString + 1, false),
        Arguments.of(randomLong, randomString, randomLong + 1, randomString + 1, false));
  }
}
