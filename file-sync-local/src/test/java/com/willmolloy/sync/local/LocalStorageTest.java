package com.willmolloy.sync.local;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.willmolloy.sync.FileTree;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * LocalStorageTest.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
@SuppressFBWarnings("UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
class LocalStorageTest {

  private FileSystem fs;
  private Path root;
  private LocalStorage sut;

  @BeforeEach
  void setUp() throws IOException {
    fs = Jimfs.newFileSystem(Configuration.forCurrentPlatform());

    root = fs.getPath("root");
    Files.createDirectory(root);

    sut = new LocalStorage(root);
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
  void scan_returnsFileTree() throws IOException {
    // Given
    Files.createFile(root.resolve("A"));
    Files.createFile(root.resolve("B"));
    Files.createDirectories(root.resolve("C/D"));
    Files.createFile(root.resolve("C/D/E"));
    Files.createFile(root.resolve("C/D/F"));
    Files.createDirectories(root.resolve("X/Y"));
    Files.createFile(root.resolve("X/Y/Z"));

    // When
    FileTree<LocalFile> scan = sut.scan();

    // Then
    assertThat(scan)
        .isEqualTo(
            FileTree.builder(LocalFile.fromPath(sut, sut.root()))
                .insert(LocalFile.fromPath(sut, root.resolve("A")))
                .insert(LocalFile.fromPath(sut, root.resolve("B")))
                .insert(LocalFile.fromPath(sut, root.resolve("C")))
                .insert(LocalFile.fromPath(sut, root.resolve("C/D")))
                .insert(LocalFile.fromPath(sut, root.resolve("C/D/E")))
                .insert(LocalFile.fromPath(sut, root.resolve("C/D/F")))
                .insert(LocalFile.fromPath(sut, root.resolve("X")))
                .insert(LocalFile.fromPath(sut, root.resolve("X/Y")))
                .insert(LocalFile.fromPath(sut, root.resolve("X/Y/Z")))
                .build());
  }

  @Test
  void toString_includesRootPath() {
    assertThat(sut.toString()).isEqualTo("LocalStorage[root]");
  }

  @Test
  void constructor_requiresValidDirectory() {
    IllegalArgumentException thrown =
        assertThrows(
            IllegalArgumentException.class,
            () -> new LocalStorage(Files.createFile(root.resolve("A"))));
    assertThat(thrown)
        .hasMessageThat()
        .isEqualTo("Requires a directory: [%s]".formatted(root.resolve("A")));
  }
}
