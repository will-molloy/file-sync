package com.willmolloy.backup.s3;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.github.javafaker.Faker;
import com.willmolloy.backup.FileTree;
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
  void isDirectory_alwaysFalse() {
    // Given
    S3Object s3Object = S3Object.builder().build();
    S3File file = new S3File(s3Object);

    // Then
    assertThat(file.isDirectory()).isFalse();
  }

  @ParameterizedTest
  @MethodSource
  void same_onlyTrueIfSizeEqual(long thisSize, long otherSize, boolean expected) {
    // Given
    S3File thisFile = spy(new S3File(S3Object.builder().size(thisSize).build()));

    FileTree.File otherFile = mock(FileTree.File.class);
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
  void toString_includesS3Object() {
    S3Object s3Object = S3Object.builder().build();
    S3File file = new S3File(s3Object);
    assertThat(file.toString()).isEqualTo("S3File[s3Object=S3Object()]");
  }
}
