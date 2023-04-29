package com.willmolloy.backup.filesystem;

import static com.google.common.truth.Truth8.assertThat;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
  void scan_walksRoot_andRelativizesTheResult() throws IOException {
    // Given
    Files.createFile(root.resolve("A"));
    Files.createFile(root.resolve("B"));
    Files.createDirectories(root.resolve("C/D"));
    Files.createFile(root.resolve("E"));
    Files.createDirectories(root.resolve("F/G/H/I"));
    Files.createDirectories(root.resolve("X/Y/Z"));

    // When
    Stream<Path> paths = sut.scan();

    // Then
    assertThat(paths)
        .containsExactly(
            fs.getPath("A"),
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
}
