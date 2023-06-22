package com.willmolloy.sync.util;

import static com.google.common.truth.Truth.assertThat;

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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Md5HelperTest.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
@SuppressFBWarnings("UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
class Md5HelperTest {

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
  void md5Base64(String contents, String md5Digest) throws IOException {
    // Given
    Path path = Files.createFile(root.resolve("A"));
    Files.writeString(path, contents);

    // When
    String result = Md5Helper.md5Base64(path);

    // Then
    assertThat(result).isEqualTo(md5Digest);
  }

  static Stream<Arguments> md5Base64() {
    return Stream.of(
        Arguments.of("", "1B2M2Y8AsgTpgAmY7PhCfg=="),
        Arguments.of("Hello world", "PiWWCnnbxptnTNTsZ6csYg=="));
  }
}
