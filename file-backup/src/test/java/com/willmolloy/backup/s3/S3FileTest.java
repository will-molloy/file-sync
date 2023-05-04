package com.willmolloy.backup.s3;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.github.javafaker.Faker;
import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;
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
    OptionalLong size = file.size();

    // Then
    assertThat(size).hasValue(randomLong);
  }

  @Test
  void size_whenNoSizeAttribute_failsGracefully() {
    // Given
    S3Object s3Object = S3Object.builder().build();
    S3File file = new S3File(s3Object);

    // When
    OptionalLong size = assertDoesNotThrow(() -> file.size());

    // Then
    assertThat(size).isEmpty();
  }

  @Test
  void lastModified_returnsS3ObjectLastModified() {
    // Given
    Instant randomInstant = Instant.now();
    S3Object s3Object = S3Object.builder().lastModified(randomInstant).build();
    S3File file = new S3File(s3Object);

    // When
    Optional<Instant> lastModified = file.lastModified();

    // Then
    assertThat(lastModified).hasValue(randomInstant);
  }

  @Test
  void size_whenNoLastModifiedAttribute_failsGracefully() {
    // Given
    S3Object s3Object = S3Object.builder().build();
    S3File file = new S3File(s3Object);

    // When
    Optional<Instant> lastModified = assertDoesNotThrow(() -> file.lastModified());

    // Then
    assertThat(lastModified).isEmpty();
  }
}
