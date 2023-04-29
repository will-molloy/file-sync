package com.willmolloy.backup.filesystem;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.google.common.truth.StreamSubject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * FileSystemBackupTest.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
class FileSystemBackupTest {

  private java.nio.file.FileSystem fs;
  private Path sourceRoot;
  private Path destRoot;
  private FileSystemBackup sut;

  @BeforeEach
  void setUp() throws IOException {
    fs = Jimfs.newFileSystem(Configuration.unix());

    sourceRoot = fs.getPath("/source");
    Files.createDirectory(sourceRoot);

    destRoot = fs.getPath("/dest");
    Files.createDirectory(destRoot);

    sut = new FileSystemBackup(new FileSystem(sourceRoot), new FileSystem(destRoot));
  }

  @AfterEach
  void tearDown() throws IOException {
    fs.close();
  }

  @Test
  void tryCopy_whenFileOnSourceAndNotOnDestination_copiesFileFromSourceToDestination()
      throws IOException {
    // Given
    Files.createFile(sourceRoot.resolve(fs.getPath("A")));

    // When
    sut.tryCopyOrUpdate(fs.getPath("A"));

    // Then
    assertThatFileSystem()
        .containsExactly(sourceRoot.resolve(fs.getPath("A")), destRoot.resolve(fs.getPath("A")));
  }

  @Test
  void tryCopy_whenDirectoryOnSourceAndNotOnDestination_copiesDirectoryFromSourceToDestination()
      throws IOException {
    // Given
    Files.createDirectories(sourceRoot.resolve(fs.getPath("A/B/C")));

    // When
    sut.tryCopyOrUpdate(fs.getPath("A/B/C"));

    // Then
    assertThatFileSystem()
        .containsExactly(
            sourceRoot.resolve(fs.getPath("A/B/C")), destRoot.resolve(fs.getPath("A/B/C")));
  }

  @Test
  void tryCopy_whenFileNotOnSource_failsGracefully() throws IOException {
    // When
    sut.tryCopyOrUpdate(fs.getPath("A"));

    // Then
    assertThatFileSystem().isEmpty();
  }

  @Test
  void tryUpdate_whenFileOnSourceAndDestination_andDifferentFileSize_updatesFileOnDestination()
      throws IOException {
    // Given
    Path sourceFile = Files.createFile(sourceRoot.resolve(fs.getPath("A")));
    Files.writeString(sourceFile, "hello abc");
    Path destFile = Files.createFile(destRoot.resolve(fs.getPath("A")));
    Files.writeString(destFile, "hello");

    // When
    sut.tryCopyOrUpdate(fs.getPath("A"));

    // Then
    assertThatFileSystem()
        .containsExactly(sourceRoot.resolve(fs.getPath("A")), destRoot.resolve(fs.getPath("A")));
  }

  @Test
  void tryUpdate_whenFileOnSourceAndDestination_andDifferentModifiedTime_updatesFileOnDestination()
      throws IOException {
    // Given
    Path sourceFile = Files.createFile(sourceRoot.resolve(fs.getPath("A")));
    Files.writeString(sourceFile, "hello");
    Path destFile = Files.createFile(destRoot.resolve(fs.getPath("A")));
    Files.writeString(destFile, "hello");

    // When
    sut.tryCopyOrUpdate(fs.getPath("A"));

    // Then
    assertThatFileSystem()
        .containsExactly(sourceRoot.resolve(fs.getPath("A")), destRoot.resolve(fs.getPath("A")));
  }

  @Test
  void tryUpdate_whenDirectoryOnSourceAndDestination_skipsUpdate() throws IOException {
    // Given
    Files.createDirectories(sourceRoot.resolve(fs.getPath("A/B/C")));
    Files.createDirectories(destRoot.resolve(fs.getPath("A/B/C")));

    // When
    sut.tryCopyOrUpdate(fs.getPath("A/B/C"));

    // Then
    assertThatFileSystem()
        .containsExactly(
            sourceRoot.resolve(fs.getPath("A/B/C")), destRoot.resolve(fs.getPath("A/B/C")));
  }

  @Test
  void tryUpdate_whenFileOnSourceAndDirectoryOnDestination_overwritesDirectoryOnDestination()
      throws IOException {
    // Given
    Files.createDirectory(sourceRoot.resolve("A"));
    Files.createFile(sourceRoot.resolve("A/B"));
    Files.createDirectories(destRoot.resolve("A/B/C"));

    // When
    sut.tryCopyOrUpdate(fs.getPath("A/B"));

    // Then
    assertThatFileSystem().containsExactly(sourceRoot.resolve("A/B"), destRoot.resolve("A/B"));
  }

  @Test
  void tryUpdate_whenFileNotOnSource_failsGracefully() throws IOException {
    // Given
    Files.createFile(destRoot.resolve(fs.getPath("A")));

    // When
    sut.tryCopyOrUpdate(fs.getPath("A"));

    // Then
    assertThatFileSystem().containsExactly(destRoot.resolve(fs.getPath("A")));
  }

  @Test
  void tryDelete_whenFileOnDestinationAndNotOnSource_deletesFileFromDestination()
      throws IOException {
    // Given
    Files.createFile(destRoot.resolve(fs.getPath("A")));

    // When
    sut.tryDelete(fs.getPath("A"));

    // Then
    assertThatFileSystem().isEmpty();
  }

  @Test
  void tryDelete_whenDirectoryOnDestinationAndNotOnSource_deletesDirectoryFromDestination()
      throws IOException {
    // Given
    Files.createDirectories(destRoot.resolve(fs.getPath("A/B/C")));

    // When
    sut.tryDelete(fs.getPath("A"));

    // Then
    assertThatFileSystem().isEmpty();
  }

  @Test
  void tryDelete_whenFileNotOnDestination_failsGracefully() throws IOException {
    // When
    sut.tryDelete(fs.getPath("A"));

    // Then
    assertThatFileSystem().isEmpty();
  }

  @Test
  void tryDelete_whenFileOnSource_skipsDelete() throws IOException {
    // Given
    Files.createFile(sourceRoot.resolve(fs.getPath("A")));
    Files.createFile(destRoot.resolve(fs.getPath("A")));

    // When
    sut.tryDelete(fs.getPath("A"));

    // Then
    assertThatFileSystem()
        .containsExactly(sourceRoot.resolve(fs.getPath("A")), destRoot.resolve(fs.getPath("A")));
  }

  @Test
  void toString_includesClassNamesAndRootPaths() {
    assertThat(sut.toString())
        .isEqualTo(
            "FileSystemBackup[source=FileSystem[root=/source], destination=FileSystem[root=/dest]]");
  }

  private StreamSubject assertThatFileSystem() throws IOException {
    try (Stream<Path> sourceFiles = Files.walk(sourceRoot).skip(1)) {
      try (Stream<Path> destFiles = Files.walk(destRoot).skip(1)) {
        return assertThat(Stream.concat(sourceFiles, destFiles).filter(this::isLeaf));
      }
    }
  }

  private boolean isLeaf(Path path) {
    if (Files.isRegularFile(path)) {
      return true;
    }
    try (Stream<Path> list = Files.list(path)) {
      return list.findAny().isEmpty();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
