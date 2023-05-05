package com.willmolloy.backup.s3;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.github.javafaker.Faker;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * S3FileTest.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
class S3FileTest {

  private final Faker faker = new Faker();

  @Test
  void size_returnsS3ObjectSize() {
    // Given
    long randomLong = faker.random().nextLong();
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
  void etag_returnsS3ObjectETag() {
    // Given
    String randomString = faker.code().asin();
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
}
