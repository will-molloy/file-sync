package com.willmolloy.backup.local;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.google.common.truth.StreamSubject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * LocalBackupTest.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
class LocalBackupTest {

  private FileSystem fs;
  private LocalStorage source;
  private Path sourceRoot;
  private LocalStorage destination;
  private Path destRoot;
  private LocalBackup sut;

  @BeforeEach
  void setUp() throws IOException {
    fs = Jimfs.newFileSystem(Configuration.forCurrentPlatform());

    sourceRoot = Files.createDirectory(fs.getPath("source"));
    source = new LocalStorage(sourceRoot);
    destRoot = Files.createDirectory(fs.getPath("dest"));
    destination = new LocalStorage(destRoot);
    sut = new LocalBackup(source, destination);
  }

  @AfterEach
  void tearDown() throws IOException {
    fs.close();
  }

  @Test
  void source_returnsSource() {
    assertThat(sut.source()).isSameInstanceAs(source);
  }

  @Test
  void destination_returnsDestination() {
    assertThat(sut.destination()).isSameInstanceAs(destination);
  }

  @Test
  void copy_copiesFileFromSourceToDestination() throws IOException {
    // Given
    Path sourceFile = Files.createFile(sourceRoot.resolve(fs.getPath("A")));
    Files.writeString(sourceFile, "source");

    // When
    sut.copy("A");

    // Then
    Path destFile = destRoot.resolve(fs.getPath("A"));
    assertThatFileSystem().containsExactly(sourceFile, destFile);
    assertThat(Files.readString(sourceFile)).isEqualTo("source");
    assertThat(Files.readString(destFile)).isEqualTo("source");
  }

  @Test
  void copy_copiesDirectoryFromSourceToDestination() throws IOException {
    // Given
    Path sourceDir = sourceRoot.resolve(fs.getPath("A/B/C"));
    Files.createDirectories(sourceDir);

    // When
    sut.copy("A/B/C");

    // Then
    Path destDir = destRoot.resolve(fs.getPath("A/B/C"));
    assertThatFileSystem().containsExactly(sourceDir, destDir);
    assertThat(Files.isDirectory(sourceDir)).isTrue();
    assertThat(Files.isDirectory(destDir)).isTrue();
  }

  @Test
  void copy_whenFileNotOnSource_failsGracefully() throws IOException {
    // When
    sut.copy("A");

    // Then
    assertThatFileSystem().isEmpty();
  }

  @Test
  void copy_whenDirectoryNotOnSource_failsGracefully() throws IOException {
    // When
    sut.copy("A/B/C");

    // Then
    assertThatFileSystem().isEmpty();
  }

  @Test
  void copy_whenFileAlreadyOnDestination_updatesAnyway() throws IOException {
    // Given
    Path sourceFile = Files.createFile(sourceRoot.resolve(fs.getPath("A")));
    Files.writeString(sourceFile, "source");
    Path destFile = Files.createFile(destRoot.resolve(fs.getPath("A")));
    Files.writeString(destFile, "dest");

    // When
    sut.copy("A");

    // Then
    assertThatFileSystem().containsExactly(sourceFile, destFile);
    assertThat(Files.readString(sourceFile)).isEqualTo("source");
    assertThat(Files.readString(destFile)).isEqualTo("source");
  }

  @Test
  void copy_whenDirectoryAlreadyOnDestination_updatesAnyway() throws IOException {
    // Given
    Path sourceDir = sourceRoot.resolve(fs.getPath("A/B/C"));
    Files.createDirectories(sourceDir);
    Path destDir = destRoot.resolve(fs.getPath("A/B/C"));
    Files.createDirectories(destDir);

    // When
    sut.copy("A/B/C");

    // Then
    assertThatFileSystem().containsExactly(sourceDir, destDir);
    assertThat(Files.isDirectory(sourceDir)).isTrue();
    assertThat(Files.isDirectory(destDir)).isTrue();
  }

  // TODO cases like file on source but directory on dest?

  @Test
  void update_updatesFileFromSourceToDestination() throws IOException {
    // Given
    Path sourceFile = Files.createFile(sourceRoot.resolve(fs.getPath("A")));
    Files.writeString(sourceFile, "source");
    Path destFile = Files.createFile(destRoot.resolve(fs.getPath("A")));
    Files.writeString(destFile, "dest");

    // When
    sut.update("A");

    // Then
    assertThatFileSystem().containsExactly(sourceFile, destFile);
    assertThat(Files.readString(sourceFile)).isEqualTo("source");
    assertThat(Files.readString(destFile)).isEqualTo("source");
  }

  @Test
  void update_updatesDirectoryFromSourceToDestination() throws IOException {
    // Given
    Path sourceDir = sourceRoot.resolve(fs.getPath("A/B/C"));
    Files.createDirectories(sourceDir);
    Path destDir = destRoot.resolve(fs.getPath("A/B/C"));
    Files.createDirectories(destDir);

    // When
    sut.update("A/B/C");

    // Then
    assertThatFileSystem().containsExactly(sourceDir, destDir);
    assertThat(Files.isDirectory(sourceDir)).isTrue();
    assertThat(Files.isDirectory(destDir)).isTrue();
  }

  @Test
  void update_whenFileNotOnSource_failsGracefully() throws IOException {
    // When
    sut.update("A");

    // Then
    assertThatFileSystem().isEmpty();
  }

  @Test
  void update_whenDirectoryNotOnSource_failsGracefully() throws IOException {
    // When
    sut.update("A/B/C");

    // Then
    assertThatFileSystem().isEmpty();
  }

  @Test
  void update_whenFileNotOnDestination_copiesAnyway() throws IOException {
    // Given
    Path sourceFile = Files.createFile(sourceRoot.resolve(fs.getPath("A")));
    Files.writeString(sourceFile, "source");

    // When
    sut.update("A");

    // Then
    Path destFile = destRoot.resolve(fs.getPath("A"));
    assertThatFileSystem().containsExactly(sourceFile, destFile);
    assertThat(Files.readString(sourceFile)).isEqualTo("source");
    assertThat(Files.readString(destFile)).isEqualTo("source");
  }

  @Test
  void update_whenDirectoryNotOnDestination_copiesAnyway() throws IOException {
    // Given
    Path sourceDir = sourceRoot.resolve(fs.getPath("A/B/C"));
    Files.createDirectories(sourceDir);

    // When
    sut.copy("A/B/C");

    // Then
    Path destDir = destRoot.resolve(fs.getPath("A/B/C"));
    assertThatFileSystem().containsExactly(sourceDir, destDir);
    assertThat(Files.isDirectory(sourceDir)).isTrue();
    assertThat(Files.isDirectory(destDir)).isTrue();
  }

  @Test
  void delete_deletesFileFromDestination() throws IOException {
    // Given
    Files.createFile(destRoot.resolve(fs.getPath("A")));

    // When
    sut.delete("A");

    // Then
    assertThatFileSystem().isEmpty();
  }

  @Test
  void delete_deletesDirectoryFromDestination() throws IOException {
    // Given
    Files.createDirectories(destRoot.resolve(fs.getPath("A/B/C")));

    // When
    sut.delete("A");

    // Then
    assertThatFileSystem().isEmpty();
  }

  @Test
  void delete_whenFileNotOnDestination_failsGracefully() throws IOException {
    // When
    sut.delete("A");

    // Then
    assertThatFileSystem().isEmpty();
  }

  @Test
  void toString_includesSourceAndDest() {
    assertThat(sut.toString())
        .isEqualTo("LocalBackup[source=LocalStorage[source], destination=LocalStorage[dest]]");
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
