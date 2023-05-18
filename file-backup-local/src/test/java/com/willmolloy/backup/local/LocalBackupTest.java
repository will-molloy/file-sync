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
    Path sourceFile = createFile(sourceRoot.resolve(fs.getPath("A")), "source");

    // When
    boolean result = sut.put(fs.getPath("A"));

    // Then
    assertThat(result).isTrue();
    Path expectedDestFile = destRoot.resolve(fs.getPath("A"));
    assertThatFileSystem().containsExactly(sourceFile, expectedDestFile);
    assertThat(Files.readString(sourceFile)).isEqualTo("source");
    assertThat(Files.readString(expectedDestFile)).isEqualTo("source");
  }

  @Test
  void put_copiesNestedFileFromSourceToDestination() throws IOException {
    // Given
    Path sourceFile = createFile(sourceRoot.resolve(fs.getPath("A/B/C")), "source");

    // When
    boolean result = sut.put(fs.getPath("A/B/C"));

    // Then
    assertThat(result).isTrue();
    Path expectedDestFile = destRoot.resolve(fs.getPath("A/B/C"));
    assertThatFileSystem().containsExactly(sourceFile, expectedDestFile);
    assertThat(Files.readString(sourceFile)).isEqualTo("source");
    assertThat(Files.readString(expectedDestFile)).isEqualTo("source");
  }

  @Test
  void put_copiesDirectoryFromSourceToDestination() throws IOException {
    // Given
    Path sourceDir = createDirectory(sourceRoot.resolve(fs.getPath("A/B/C")));

    // When
    boolean result = sut.put(fs.getPath("A/B/C"));

    // Then
    assertThat(result).isTrue();
    Path expectedDestDir = destRoot.resolve(fs.getPath("A/B/C"));
    assertThatFileSystem().containsExactly(sourceDir, expectedDestDir);
    assertThat(Files.isDirectory(sourceDir)).isTrue();
    assertThat(Files.isDirectory(expectedDestDir)).isTrue();
  }

  @Test
  void put_updatesFileFromSourceToDestination() throws IOException {
    // Given
    Path sourceFile = createFile(sourceRoot.resolve(fs.getPath("A")), "source");
    Path destFile = createFile(destRoot.resolve(fs.getPath("A")), "dest");

    // When
    boolean result = sut.put(fs.getPath("A"));

    // Then
    assertThat(result).isTrue();
    assertThatFileSystem().containsExactly(sourceFile, destFile);
    assertThat(Files.readString(sourceFile)).isEqualTo("source");
    assertThat(Files.readString(destFile)).isEqualTo("source");
  }

  @Test
  void put_updatesNestedFileFromSourceToDestination() throws IOException {
    // Given
    Path sourceFile = createFile(sourceRoot.resolve(fs.getPath("A/B/C")), "source");
    Path destFile = createFile(destRoot.resolve(fs.getPath("A/B/C")), "dest");

    // When
    boolean result = sut.put(fs.getPath("A/B/C"));

    // Then
    assertThat(result).isTrue();
    assertThatFileSystem().containsExactly(sourceFile, destFile);
    assertThat(Files.readString(sourceFile)).isEqualTo("source");
    assertThat(Files.readString(destFile)).isEqualTo("source");
  }

  @Test
  void put_updatesDirectoryFromSourceToDestination() throws IOException {
    // Given
    Path sourceDir = createDirectory(sourceRoot.resolve(fs.getPath("A/B/C")));
    Path destDir = createDirectory(destRoot.resolve(fs.getPath("A/B/C")));

    // When
    boolean result = sut.put(fs.getPath("A/B/C"));

    // Then
    assertThat(result).isTrue();
    assertThatFileSystem().containsExactly(sourceDir, destDir);
    assertThat(Files.isDirectory(sourceDir)).isTrue();
    assertThat(Files.isDirectory(destDir)).isTrue();
  }

  @Test
  void put_replacesNonEmptyDirectoryOnDestinationWithFileOnSource() throws IOException {
    // Given
    Path sourceFile = createFile(sourceRoot.resolve(fs.getPath("A/B/C")), "source");
    createFile(destRoot.resolve(fs.getPath("A/B/C/D/E/F")), "dest");
    createFile(destRoot.resolve(fs.getPath("A/B/C/X/Y/Z")), "dest2");

    // When
    boolean result = sut.put(fs.getPath("A/B/C"));

    // Then
    assertThat(result).isTrue();
    Path expectedDestFile = destRoot.resolve(fs.getPath("A/B/C"));
    assertThatFileSystem().containsExactly(sourceFile, expectedDestFile);
    assertThat(Files.readString(sourceFile)).isEqualTo("source");
    assertThat(Files.readString(expectedDestFile)).isEqualTo("source");
  }

  @Test
  void put_replacesFileOnDestinationWithNonEmptyDirectoryOnSource_FileAlreadyExistsException()
      throws IOException {
    // Given
    Path sourceFile = createFile(sourceRoot.resolve(fs.getPath("A/B/C/D")), "source");
    createFile(destRoot.resolve(fs.getPath("A/B/C")), "dest");

    // When
    boolean result = sut.put(fs.getPath("A/B/C/D"));

    // Then
    assertThat(result).isTrue();
    Path expectedDestFile = destRoot.resolve(fs.getPath("A/B/C/D"));
    assertThatFileSystem().containsExactly(sourceFile, expectedDestFile);
    assertThat(Files.readString(sourceFile)).isEqualTo("source");
    assertThat(Files.readString(expectedDestFile)).isEqualTo("source");
  }

  @Test
  void put_replacesFileOnDestinationWithNonEmptyDirectoryOnSource_NoSuchFileException()
      throws IOException {
    // Given
    // this is when we get NoSuchFileException from Files.createDirectories...
    Path sourceFile = createFile(sourceRoot.resolve(fs.getPath("A/B/C/D/E")), "source");
    createFile(destRoot.resolve(fs.getPath("A/B/C")), "dest");

    // When
    boolean result = sut.put(fs.getPath("A/B/C/D/E"));

    // Then
    assertThat(result).isTrue();
    Path expectedDestFile = destRoot.resolve(fs.getPath("A/B/C/D/E"));
    assertThatFileSystem().containsExactly(sourceFile, expectedDestFile);
    assertThat(Files.readString(sourceFile)).isEqualTo("source");
    assertThat(Files.readString(expectedDestFile)).isEqualTo("source");
  }

  @Test
  void put_whenFileNotOnSource_failsGracefully() throws IOException {
    // When
    boolean result = assertDoesNotThrow(() -> sut.put(fs.getPath("A")));

    // Then
    assertThat(result).isTrue();
    assertThatFileSystem().isEmpty();
  }

  @Test
  void delete_deletesFileFromDestination() throws IOException {
    // Given
    createFile(destRoot.resolve(fs.getPath("A")), "");

    // When
    boolean result = sut.delete(fs.getPath("A"));

    // Then
    assertThat(result).isTrue();
    assertThatFileSystem().isEmpty();
  }

  @Test
  void delete_deletesDirectoryFromDestination() throws IOException {
    // Given
    createDirectory(destRoot.resolve(fs.getPath("A/B/C")));
    createDirectory(destRoot.resolve(fs.getPath("A/X/Y/Z")));

    // When
    boolean result = sut.delete(fs.getPath("A"));

    // Then
    assertThat(result).isTrue();
    assertThatFileSystem().isEmpty();
  }

  @Test
  void delete_whenFileNotOnDestination_failsGracefully() throws IOException {
    // When
    boolean result = assertDoesNotThrow(() -> sut.delete(fs.getPath("A")));

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

  private Path createFile(Path path, String contents) throws IOException {
    Path parent = path.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    Files.createFile(path);
    Files.writeString(path, contents);
    return path;
  }

  private Path createDirectory(Path path) throws IOException {
    Files.createDirectories(path);
    return path;
  }
}
