package com.willmolloy.backup.local;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.google.common.truth.StreamSubject;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
@SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
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
  void put_copiesFileFromSourceToDestination() throws IOException {
    // Given
    Path sourceFile = Files.createFile(sourceRoot.resolve(fs.getPath("A")));
    Files.writeString(sourceFile, "source");

    // When
    boolean result = sut.put("A");

    // Then
    assertThat(result).isTrue();
    Path destFile = destRoot.resolve(fs.getPath("A"));
    assertThatFileSystem().containsExactly(sourceFile, destFile);
    assertThat(Files.readString(sourceFile)).isEqualTo("source");
    assertThat(Files.readString(destFile)).isEqualTo("source");
  }

  @Test
  void put_copiesNestedFileFromSourceToDestination() throws IOException {
    // Given
    Path sourceFile = sourceRoot.resolve(fs.getPath("A/B/C"));
    Files.createDirectories(sourceFile.getParent());
    Files.createFile(sourceFile);
    Files.writeString(sourceFile, "source");

    // When
    boolean result = sut.put("A/B/C");

    // Then
    assertThat(result).isTrue();
    Path destFile = destRoot.resolve(fs.getPath("A/B/C"));
    assertThatFileSystem().containsExactly(sourceFile, destFile);
    assertThat(Files.readString(sourceFile)).isEqualTo("source");
    assertThat(Files.readString(destFile)).isEqualTo("source");
  }

  @Test
  void put_updatesFileFromSourceToDestination() throws IOException {
    // Given
    Path sourceFile = Files.createFile(sourceRoot.resolve(fs.getPath("A")));
    Files.writeString(sourceFile, "source");
    Path destFile = Files.createFile(destRoot.resolve(fs.getPath("A")));
    Files.writeString(destFile, "dest");

    // When
    boolean result = sut.put("A");

    // Then
    assertThat(result).isTrue();
    assertThatFileSystem().containsExactly(sourceFile, destFile);
    assertThat(Files.readString(sourceFile)).isEqualTo("source");
    assertThat(Files.readString(destFile)).isEqualTo("source");
  }

  @Test
  void put_updatesNestedFileFromSourceToDestination() throws IOException {
    // Given
    Path sourceFile = sourceRoot.resolve(fs.getPath("A/B/C"));
    Files.createDirectories(sourceFile.getParent());
    Files.createFile(sourceFile);
    Files.writeString(sourceFile, "source");

    Path destFile = destRoot.resolve(fs.getPath("A/B/C"));
    Files.createDirectories(destFile.getParent());
    Files.createFile(destFile);
    Files.writeString(destFile, "dest");

    // When
    boolean result = sut.put("A/B/C");

    // Then
    assertThat(result).isTrue();
    assertThatFileSystem().containsExactly(sourceFile, destFile);
    assertThat(Files.readString(sourceFile)).isEqualTo("source");
    assertThat(Files.readString(destFile)).isEqualTo("source");
  }

  @Test
  void put_overwritesNonEmptyDirectoryOnDestinationWithFileOnSource() throws IOException {
    // Given
    Path sourceFile = sourceRoot.resolve(fs.getPath("A/B/C"));
    Files.createDirectories(sourceFile.getParent());
    Files.createFile(sourceFile);
    Files.writeString(sourceFile, "source");

    Path destDir = destRoot.resolve(fs.getPath("A/B/C/D"));
    Files.createDirectories(destDir);

    // When
    boolean result = sut.put("A/B/C");

    // Then
    Path expectedDestFile = destRoot.resolve(fs.getPath("A/B/C"));
    assertThat(result).isTrue();
    assertThatFileSystem().containsExactly(sourceFile, expectedDestFile);
    assertThat(Files.readString(sourceFile)).isEqualTo("source");
    assertThat(Files.readString(expectedDestFile)).isEqualTo("source");
  }

  @Test
  void put_whenFileNotOnSource_failsGracefully() throws IOException {
    // When
    boolean result = assertDoesNotThrow(() -> sut.put("A"));

    // Then
    assertThat(result).isFalse();
    assertThatFileSystem().isEmpty();
  }

  @Test
  void delete_deletesFileFromDestination() throws IOException {
    // Given
    Files.createFile(destRoot.resolve(fs.getPath("A")));

    // When
    boolean result = sut.delete("A");

    // Then
    assertThat(result).isTrue();
    assertThatFileSystem().isEmpty();
  }

  @Test
  void delete_deletesDirectoryFromDestination() throws IOException {
    // Given
    Files.createDirectories(destRoot.resolve(fs.getPath("A/B/C")));

    // When
    boolean result = sut.delete("A");

    // Then
    assertThat(result).isTrue();
    assertThatFileSystem().isEmpty();
  }

  @Test
  void delete_whenFileNotOnDestination_failsGracefully() throws IOException {
    // When
    boolean result = assertDoesNotThrow(() -> sut.delete("A"));

    // Then
    assertThat(result).isTrue();
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
