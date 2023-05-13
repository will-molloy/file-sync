package com.willmolloy.backup.local;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * LocalDirectoryTest.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
@SuppressFBWarnings("UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
class LocalDirectoryTest {

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

  @Test
  void toString_includesPath() throws IOException {
    Path path = Files.createDirectory(root.resolve("ABCD"));
    LocalDirectory file = new LocalDirectory(path);
    assertThat(file.toString()).isEqualTo("LocalDirectory[%s]".formatted(path));
  }

  @Test
  void constructor_requiresValidFile() {
    IllegalArgumentException thrown =
        assertThrows(
            IllegalArgumentException.class,
            () -> new LocalDirectory(Files.createFile(root.resolve("A"))));
    assertThat(thrown)
        .hasMessageThat()
        .isEqualTo("Requires a directory: [%s]".formatted(root.resolve("A")));
  }
}
