package com.willmolloy.backup.local;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.willmolloy.backup.Backup.File;
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
    fs = Jimfs.newFileSystem(Configuration.unix());

    root = fs.getPath("/root");
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
  void scan_returnsMapOfRelativizedFileNamesToFiles() throws IOException {
    // Given
    Path a = Files.createFile(root.resolve("A"));
    Path b = Files.createFile(root.resolve("B"));
    Files.createDirectories(root.resolve("C/D"));
    Path e = Files.createFile(root.resolve("C/D/E"));
    Files.createDirectories(root.resolve("F/G/H"));
    Path i = Files.createFile(root.resolve("F/G/H/I"));
    Files.createDirectories(root.resolve("X/Y"));
    Path z = Files.createFile(root.resolve("X/Y/Z"));

    // When
    Map<String, File> scan = sut.scan();

    // Then
    assertThat(scan)
        .containsExactly(
            "A", new LocalFile(a),
            "B", new LocalFile(b),
            "C/D/E", new LocalFile(e),
            "F/G/H/I", new LocalFile(i),
            "X/Y/Z", new LocalFile(z));
  }

  @Test
  void toString_includesClassNameAndRootPath() {
    assertThat(sut.toString()).isEqualTo("LocalStorage[root=/root]");
  }
}
