package com.willmolloy.backup.local;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.collect.Range;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * LocalFileTest.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
@SuppressFBWarnings("UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
class LocalFileTest {

  private FileSystem fs;
  private Path root;

  @BeforeEach
  void setUp() throws IOException {
    fs = Jimfs.newFileSystem(Configuration.forCurrentPlatform());

    root = fs.getPath("root");
    Files.createDirectory(root);
  }

  @AfterEach
  void tearDown() throws IOException {
    fs.close();
  }

  @ParameterizedTest
  @MethodSource
  void size_returnsSizeOfFileInBytes(String contents, int size) throws IOException {
    // Given
    LocalFile file = new LocalFile(Files.createFile(root.resolve("A")));
    Files.writeString(file.path(), contents);

    // When
    long result = file.size();

    // Then
    assertThat(result).isEqualTo(size);
  }

  static Stream<Arguments> size_returnsSizeOfFileInBytes() {
    return Stream.of(Arguments.of("", 0), Arguments.of("Hello world", 11));
  }

  @Test
  void size_whenPathDoesntExist_failsGracefully() throws IOException {
    // Given
    LocalFile file = new LocalFile(Files.createFile(root.resolve("A")));
    Files.delete(file.path());

    // When
    long result = file.size();

    // Then
    assertThat(result).isEqualTo(-1);
  }

  @Test
  void lastModified_returnsLastModifiedInstant() throws IOException {
    // Given
    LocalFile file = new LocalFile(Files.createFile(root.resolve("A")));

    // When
    Instant result = file.lastModified();

    // Then
    long tolerance = 100;
    assertThat(result).isIn(Range.closed(result.minusMillis(tolerance), result.plusMillis(100)));
  }

  @Test
  void lastModified_whenPathDoesntExist_failsGracefully() throws IOException {
    // Given
    LocalFile file = new LocalFile(Files.createFile(root.resolve("A")));
    Files.delete(file.path());

    // When
    Instant result = file.lastModified();

    // Then
    assertThat(result).isEqualTo(Instant.MIN);
  }

  @Test
  void constructor_requiresValidFile() {
    IllegalArgumentException thrown =
        assertThrows(
            IllegalArgumentException.class,
            () -> new LocalFile(Files.createDirectory(root.resolve("A"))));
    assertThat(thrown)
        .hasMessageThat()
        .isEqualTo("Requires a file: [%s]".formatted(root.resolve("A")));
  }
}
