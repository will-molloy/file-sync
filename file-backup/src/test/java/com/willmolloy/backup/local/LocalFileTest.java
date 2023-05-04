package com.willmolloy.backup.local;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
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
  void etag_returnsMd5HashOfFileContentsInBase16(String contents, String md5Digest)
      throws IOException {
    // Given
    LocalFile file = new LocalFile(Files.createFile(root.resolve("A")));
    Files.writeString(file.path(), contents);

    // When
    String result = file.etag();

    // Then
    assertThat(result).isEqualTo(md5Digest);
  }

  static Stream<Arguments> etag_returnsMd5HashOfFileContentsInBase16() {
    return Stream.of(
        Arguments.of("", "\"d41d8cd98f00b204e9800998ecf8427e\""),
        Arguments.of("Hello world", "\"3e25960a79dbc69b674cd4ec67a72c62\""));
  }

  @Test
  void etag_whenPathDoesntExist_failsGracefully() throws IOException {
    // Given
    LocalFile file = new LocalFile(Files.createFile(root.resolve("A")));
    Files.delete(file.path());

    // When
    String result = assertDoesNotThrow(() -> file.etag());

    // Then
    assertThat(result).isEmpty();
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
