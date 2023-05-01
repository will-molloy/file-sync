package com.willmolloy.backup.filesystem;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

import com.google.common.collect.Range;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.willmolloy.backup.Backup;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * FileSystemTest.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
@SuppressFBWarnings("UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
class FileSystemTest {

  private java.nio.file.FileSystem fs;
  private Path root;
  private FileSystem sut;

  @BeforeEach
  void setUp() throws IOException {
    fs = Jimfs.newFileSystem(Configuration.unix());

    root = fs.getPath("/root");
    Files.createDirectory(root);

    sut = new FileSystem(root);
  }

  @AfterEach
  void tearDown() throws IOException {
    fs.close();
  }

  @Test
  void root_returnsRootDir() {
    assertThat(sut.root()).isSameInstanceAs(root);
  }

  @Test
  void scan_walksRoot_andRelativizesThePaths() throws IOException {
    // Given
    Files.createFile(root.resolve("A"));
    Files.createFile(root.resolve("B"));
    Files.createDirectories(root.resolve("C/D"));
    Files.createFile(root.resolve("E"));
    Files.createDirectories(root.resolve("F/G/H/I"));
    Files.createDirectories(root.resolve("X/Y/Z"));

    // When
    Map<String, Backup.File> scan = sut.scan();

    // Then
    assertThat(scan)
        .containsExactly(
            "A", root.resolve("A"),
            fs.getPath("B"),
            fs.getPath("C"),
            fs.getPath("C/D"),
            fs.getPath("E"),
            fs.getPath("F"),
            fs.getPath("F/G"),
            fs.getPath("F/G/H"),
            fs.getPath("F/G/H/I"),
            fs.getPath("X"),
            fs.getPath("X/Y"),
            fs.getPath("X/Y/Z"));
  }

  @ParameterizedTest
  @MethodSource
  void size_returnsSizeOfFileInBytes(String contents, int size) throws IOException {
    // Given
    Path file = Files.createFile(root.resolve("A"));
    Files.writeString(file, contents);

    // When
    long result = sut.size(fs.getPath("A"));

    // Then
    assertThat(result).isEqualTo(size);
  }

  static Stream<Arguments> size_returnsSizeOfFileInBytes() {
    return Stream.of(Arguments.of("", 0), Arguments.of("Hello world", 11));
  }

  @Test
  void size_whenPathDoesntExist_failsGracefully() {
    // When
    long result = sut.size(fs.getPath("A"));

    // Then
    assertThat(result).isEqualTo(-1);
  }

  @Test
  void lastModified_returnsLastModifiedTimeInMillis() throws IOException {
    // Given
    Files.createFile(root.resolve("A"));

    // When
    long result = sut.lastModified(fs.getPath("A"));

    // Then
    long currentMillis = System.currentTimeMillis();
    long tolerance = 100;
    assertThat(result).isIn(Range.closed(currentMillis - tolerance, currentMillis + tolerance));
  }

  @Test
  void lastModified_whenPathDoesntExist_failsGracefully() {
    // When
    long result = sut.lastModified(fs.getPath("A"));

    // Then
    assertThat(result).isEqualTo(-1);
  }

  @Test
  void toString_includesClassNameAndRootPath() {
    assertThat(sut.toString()).isEqualTo("FileSystem[root=/root]");
  }
}
