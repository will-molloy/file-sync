package com.willmolloy.backup;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.google.common.truth.StreamSubject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
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

  private FileSystem fileSystem;
  private Path sourceRoot;
  private Path destinationRoot;

  private FileSystemBackup sut;

  @BeforeEach
  void setUp() throws IOException {
    fileSystem = Jimfs.newFileSystem(Configuration.unix());

    sourceRoot = fileSystem.getPath("/source");
    Files.createDirectory(sourceRoot);

    destinationRoot = fileSystem.getPath("/dest");
    Files.createDirectory(destinationRoot);

    sut = new FileSystemBackup(sourceRoot, destinationRoot);
  }

  @AfterEach
  void tearDown() throws IOException {
    fileSystem.close();
  }

  @Test
  void scanSource_walksRoot_andRelativizesTheResult() throws IOException {
    // Given
    Files.createFile(sourceRoot.resolve("A"));
    Files.createFile(sourceRoot.resolve("B"));
    Files.createDirectories(sourceRoot.resolve("C/D"));
    Files.createFile(sourceRoot.resolve("E"));
    Files.createDirectories(sourceRoot.resolve("F/G/H/I"));
    Files.createDirectories(sourceRoot.resolve("X/Y/Z"));

    // When
    Stream<Path> paths = sut.scanSource();

    // Then
    assertThat(paths)
        .containsExactly(
            fileSystem.getPath("A"),
            fileSystem.getPath("B"),
            fileSystem.getPath("C"),
            fileSystem.getPath("C/D"),
            fileSystem.getPath("E"),
            fileSystem.getPath("F"),
            fileSystem.getPath("F/G"),
            fileSystem.getPath("F/G/H"),
            fileSystem.getPath("F/G/H/I"),
            fileSystem.getPath("X"),
            fileSystem.getPath("X/Y"),
            fileSystem.getPath("X/Y/Z"));
  }

  @Test
  void scanDestination_walksRoot_andRelativizesTheResult() throws IOException {
    // Given
    Files.createFile(destinationRoot.resolve("A"));
    Files.createFile(destinationRoot.resolve("B"));
    Files.createDirectories(destinationRoot.resolve("C/D"));
    Files.createFile(destinationRoot.resolve("E"));
    Files.createDirectories(destinationRoot.resolve("F/G/H/I"));
    Files.createDirectories(destinationRoot.resolve("X/Y/Z"));

    // When
    Stream<Path> paths = sut.scanDestination();

    // Then
    assertThat(paths)
        .containsExactly(
            fileSystem.getPath("A"),
            fileSystem.getPath("B"),
            fileSystem.getPath("C"),
            fileSystem.getPath("C/D"),
            fileSystem.getPath("E"),
            fileSystem.getPath("F"),
            fileSystem.getPath("F/G"),
            fileSystem.getPath("F/G/H"),
            fileSystem.getPath("F/G/H/I"),
            fileSystem.getPath("X"),
            fileSystem.getPath("X/Y"),
            fileSystem.getPath("X/Y/Z"));
  }

  @Test
  void copy_copiesFileFromSourceToDestination() throws IOException {
    // Given
    Path relativeFile = fileSystem.getPath("A");
    Files.createFile(sourceRoot.resolve(relativeFile));

    // When
    sut.copy(relativeFile);

    // Then
    assertThatFileSystem()
        .containsExactly(sourceRoot.resolve(relativeFile), destinationRoot.resolve(relativeFile));
  }

  @Test
  void copy_copiesDirectoryFromSourceToDestination() throws IOException {
    // Given
    Path relativeFile = fileSystem.getPath("A/B/C");
    Files.createDirectories(sourceRoot.resolve(relativeFile));

    // When
    sut.copy(relativeFile);

    // Then
    assertThatFileSystem()
        .containsExactly(sourceRoot.resolve(relativeFile), destinationRoot.resolve(relativeFile));
  }

  @Test
  void copy_whenFileDoesntExistOnSource_failsToCopy() {
    // Given
    Path relativeFile = fileSystem.getPath("A");

    // When
    UncheckedIOException thrown =
        assertThrows(UncheckedIOException.class, () -> sut.copy(relativeFile));

    // Then
    assertThat(thrown).hasCauseThat().isInstanceOf(NoSuchFileException.class);
  }

  @Test
  void copy_whenFileExistsOnDestination_failsToCopy() throws IOException {
    // Given
    Path relativeFile = fileSystem.getPath("A");
    Files.createFile(sourceRoot.resolve(relativeFile));
    Files.createFile(destinationRoot.resolve(relativeFile));

    // When
    UncheckedIOException thrown =
        assertThrows(UncheckedIOException.class, () -> sut.copy(relativeFile));

    // Then
    assertThat(thrown).hasCauseThat().isInstanceOf(FileAlreadyExistsException.class);
  }

  @Test
  void delete_deletesFileFromDestination() throws IOException {
    // Given
    Path relativeFile = fileSystem.getPath("A");
    Files.createFile(destinationRoot.resolve(relativeFile));

    // When
    sut.delete(relativeFile);

    // Then
    assertThatFileSystem().isEmpty();
  }

  @Test
  void delete_deletesDirectoryFromDestination() throws IOException {
    // Given
    Path relativeFile = fileSystem.getPath("A/B/C");
    Files.createDirectories(destinationRoot.resolve(relativeFile));

    // When
    sut.delete(fileSystem.getPath("A"));

    // Then
    assertThatFileSystem().isEmpty();
  }

  // TODO how to test the equals call? Need to mock Files.copy such that it can be verified.
  @Test
  void update_updatesFileOnDestination() throws IOException {
    // Given
    Path relativeFile = fileSystem.getPath("A");
    Files.createFile(sourceRoot.resolve(relativeFile));
    Files.createFile(destinationRoot.resolve(relativeFile));

    // When
    sut.update(relativeFile);

    // Then
    assertThatFileSystem()
        .containsExactly(sourceRoot.resolve(relativeFile), destinationRoot.resolve(relativeFile));
  }

  @Test
  void update_updatesDirectoryOnDestination() throws IOException {
    // Given
    Path relativeFile = fileSystem.getPath("A/B/C");
    Files.createDirectories(sourceRoot.resolve(relativeFile));
    Files.createDirectories(destinationRoot.resolve(relativeFile));

    // When
    sut.update(relativeFile);

    // Then
    assertThatFileSystem()
        .containsExactly(sourceRoot.resolve(relativeFile), destinationRoot.resolve(relativeFile));
  }

  @Test
  void update_whenFileDoesntExistOnSource_failsToUpdate() throws IOException {
    // Given
    Path relativeFile = fileSystem.getPath("A");
    Files.createFile(sourceRoot.resolve(relativeFile));

    // When
    UncheckedIOException thrown =
        assertThrows(UncheckedIOException.class, () -> sut.update(relativeFile));

    // Then
    assertThat(thrown).hasCauseThat().isInstanceOf(NoSuchFileException.class);
  }

  @Test
  void update_whenFileDoesntExistOnDestination_failsToUpdate() throws IOException {
    // Given
    Path relativeFile = fileSystem.getPath("A");
    Files.createFile(destinationRoot.resolve(relativeFile));

    // When
    UncheckedIOException thrown =
        assertThrows(UncheckedIOException.class, () -> sut.update(relativeFile));

    // Then
    assertThat(thrown).hasCauseThat().isInstanceOf(NoSuchFileException.class);
  }

  private StreamSubject assertThatFileSystem() throws IOException {
    try (Stream<Path> sourceFiles = Files.walk(sourceRoot).skip(1)) {
      try (Stream<Path> destinationFiles = Files.walk(destinationRoot).skip(1)) {
        return assertThat(Stream.concat(sourceFiles, destinationFiles).filter(this::isLeaf));
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
