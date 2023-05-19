package com.willmolloy.backup.local;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.willmolloy.backup.FileTree;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
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
  void scan_returnsMapOfRelativizedFileNamesToFilesAndDirectories() throws IOException {
    // Given
    Files.createFile(root.resolve("A"));
    Files.createFile(root.resolve("B"));
    Files.createDirectories(root.resolve("C/D"));
    Files.createFile(root.resolve("C/D/E"));
    Files.createDirectories(root.resolve("F/G/H"));
    Files.createFile(root.resolve("F/G/H/I"));
    Files.createDirectories(root.resolve("X/Y"));
    Files.createFile(root.resolve("X/Y/Z"));

    // When
    FileTree scan = sut.scan();

    // Then
    assertThat(scan)
        .isEqualTo(
            FileTree.from(
                Map.ofEntries(
                    entry(fs.getPath("A"), new LocalFile(root.resolve("A"))),
                    entry(fs.getPath("B"), new LocalFile(root.resolve("B"))),
                    entry(fs.getPath("C"), new LocalFile(root.resolve("C"))),
                    entry(fs.getPath("C/D"), new LocalFile(root.resolve("C/D"))),
                    entry(fs.getPath("C/D/E"), new LocalFile(root.resolve("C/D/E"))),
                    entry(fs.getPath("F"), new LocalFile(root.resolve("F"))),
                    entry(fs.getPath("F/G"), new LocalFile(root.resolve("F/G"))),
                    entry(fs.getPath("F/G/H"), new LocalFile(root.resolve("F/G/H"))),
                    entry(fs.getPath("F/G/H/I"), new LocalFile(root.resolve("F/G/H/I"))),
                    entry(fs.getPath("X"), new LocalFile(root.resolve("X"))),
                    entry(fs.getPath("X/Y"), new LocalFile(root.resolve("X/Y"))),
                    entry(fs.getPath("X/Y/Z"), new LocalFile(root.resolve("X/Y/Z"))))));
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
