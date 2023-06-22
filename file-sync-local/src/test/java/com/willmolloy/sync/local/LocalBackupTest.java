package com.willmolloy.sync.local;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.google.common.io.MoreFiles;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.google.common.truth.StreamSubject;
import com.willmolloy.sync.statistics.LoggingBackupObserver;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * LocalBackupTest.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
@SuppressFBWarnings({"UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", "DLS_DEAD_LOCAL_STORE"})
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
    sut = new LocalBackup(source, destination, List.of(new LoggingBackupObserver()));
  }

  @AfterEach
  void tearDown() throws IOException {
    fs.close();
  }

  @Test
  void put_copiesFileFromSourceToDestination() throws IOException {
    // Given
    Path relativePath = fs.getPath("A");
    LocalFile sourceFile = createFile(source, relativePath, "source");

    // When
    boolean result = sut.put(sourceFile);

    // Then
    assertThat(result).isTrue();
    Path sourcePath = sourceRoot.resolve(relativePath);
    Path destPath = destRoot.resolve(relativePath);
    assertThatFileSystem().containsExactly(sourcePath, destPath);
    assertThat(Files.readString(sourcePath)).isEqualTo("source");
    assertThat(Files.readString(destPath)).isEqualTo("source");
  }

  @Test
  void put_copiesNestedFileFromSourceToDestination() throws IOException {
    // Given
    Path relativePath = fs.getPath("A/B/C");
    LocalFile sourceFile = createFile(source, relativePath, "source");

    // When
    boolean result = sut.put(sourceFile);

    // Then
    assertThat(result).isTrue();
    Path sourcePath = sourceRoot.resolve(relativePath);
    Path destPath = destRoot.resolve(relativePath);
    assertThatFileSystem().containsExactly(sourcePath, destPath);
    assertThat(Files.readString(sourcePath)).isEqualTo("source");
    assertThat(Files.readString(destPath)).isEqualTo("source");
  }

  @Test
  void put_copiesDirectoryFromSourceToDestination() throws IOException {
    // Given
    Path relativePath = fs.getPath("A/B/C");
    LocalFile sourceDir = createDirectory(source, relativePath);

    // When
    boolean result = sut.put(sourceDir);

    // Then
    assertThat(result).isTrue();
    Path sourcePath = sourceRoot.resolve(relativePath);
    Path destPath = destRoot.resolve(relativePath);
    assertThatFileSystem().containsExactly(sourcePath, destPath);
    assertThat(Files.isDirectory(sourcePath)).isTrue();
    assertThat(Files.isDirectory(destPath)).isTrue();
  }

  @Test
  void put_updatesFileFromSourceToDestination() throws IOException {
    // Given
    Path relativePath = fs.getPath("A");
    LocalFile sourceFile = createFile(source, relativePath, "source");
    LocalFile destFile = createFile(destination, relativePath, "dest");

    // When
    boolean result = sut.put(sourceFile);

    // Then
    assertThat(result).isTrue();
    Path sourcePath = sourceRoot.resolve(relativePath);
    Path destPath = destRoot.resolve(relativePath);
    assertThatFileSystem().containsExactly(sourcePath, destPath);
    assertThat(Files.readString(sourcePath)).isEqualTo("source");
    assertThat(Files.readString(destPath)).isEqualTo("source");
  }

  @Test
  void put_updatesNestedFileFromSourceToDestination() throws IOException {
    // Given
    Path relativePath = fs.getPath("A/B/C");
    LocalFile sourceFile = createFile(source, relativePath, "source");
    LocalFile destFile = createFile(destination, relativePath, "dest");

    // When
    boolean result = sut.put(sourceFile);

    // Then
    assertThat(result).isTrue();
    Path sourcePath = sourceRoot.resolve(relativePath);
    Path destPath = destRoot.resolve(relativePath);
    assertThatFileSystem().containsExactly(sourcePath, destPath);
    assertThat(Files.readString(sourcePath)).isEqualTo("source");
    assertThat(Files.readString(destPath)).isEqualTo("source");
  }

  @Test
  void put_updatesDirectoryFromSourceToDestination() throws IOException {
    // Given
    Path relativePath = fs.getPath("A/B/C");
    LocalFile sourceDir = createDirectory(source, relativePath);
    LocalFile destDir = createDirectory(destination, relativePath);

    // When
    boolean result = sut.put(sourceDir);

    // Then
    assertThat(result).isTrue();
    Path sourcePath = sourceRoot.resolve(relativePath);
    Path destPath = destRoot.resolve(relativePath);
    assertThatFileSystem().containsExactly(sourcePath, destPath);
    assertThat(Files.isDirectory(sourcePath)).isTrue();
    assertThat(Files.isDirectory(destPath)).isTrue();
  }

  @Test
  void put_whenFileNotOnSource_failsGracefully() throws IOException {
    // Given
    LocalFile sourceFile = createFile(source, fs.getPath("A"), "source");
    Files.delete(sourceFile.fullPath());

    // When
    boolean result = assertDoesNotThrow(() -> sut.put(sourceFile));

    // Then
    assertThat(result).isTrue();
    assertThatFileSystem().isEmpty();
  }

  @Test
  void delete_deletesFileFromDestination() throws IOException {
    // Given
    LocalFile destFile = createFile(destination, fs.getPath("A"), "dest");

    // When
    boolean result = sut.delete(destination.scan().subtree(destFile));

    // Then
    assertThat(result).isTrue();
    assertThatFileSystem().isEmpty();
  }

  @Test
  void delete_deletesDirectoryFromDestination() throws IOException {
    // Given
    LocalFile destDir = createDirectory(destination, fs.getPath("A"));
    createFile(destination, fs.getPath("A/B/C"), "dest");
    createFile(destination, fs.getPath("A/X/Y/Z"), "dest");

    // When
    boolean result = sut.delete(destination.scan().subtree(destDir));

    // Then
    assertThat(result).isTrue();
    assertThatFileSystem().isEmpty();
  }

  @ParameterizedTest
  @MethodSource
  void needUpdate_trueWhenSizeOrLastModifiedNotEqual(
      String sourceContents,
      long sourceLastModified,
      String destContents,
      long destLastModified,
      boolean expected)
      throws IOException {
    // Given
    Path sourcePath = Files.createFile(source.root().resolve("A"));
    Files.writeString(sourcePath, sourceContents);
    Files.setLastModifiedTime(sourcePath, FileTime.fromMillis(sourceLastModified));
    LocalFile sourceFile = LocalFile.fromPath(source, sourcePath);

    Path destPath = Files.createFile(destination.root().resolve("B"));
    Files.writeString(destPath, destContents);
    Files.setLastModifiedTime(destPath, FileTime.fromMillis(destLastModified));
    LocalFile destFile = LocalFile.fromPath(destination, destPath);

    // Then
    assertThat(sut.needUpdate(sourceFile, destFile)).isEqualTo(expected);
  }

  static Stream<Arguments> needUpdate_trueWhenSizeOrLastModifiedNotEqual() {
    long currentMillis = System.currentTimeMillis();
    return Stream.of(
        Arguments.of("ABC", currentMillis, "ABC", currentMillis, false),
        Arguments.of("ABC", currentMillis, "XYZ", currentMillis, false),
        Arguments.of("ABC", currentMillis, "ABCD", currentMillis, true),
        Arguments.of("ABC", currentMillis, "ABC", currentMillis + 1, true),
        Arguments.of("ABC", currentMillis, "XYZ", currentMillis + 1, true),
        Arguments.of("ABC", currentMillis, "ABCD", currentMillis + 1, true));
  }

  @Test
  void needUpdate_whenFileOnSourceAndDirectoryOnDestination_true() throws IOException {
    // Given
    Path relativePath = fs.getPath("A");
    LocalFile sourceFile = createFile(source, relativePath, "source");
    LocalFile destDir = createDirectory(destination, relativePath);

    // Then
    assertThat(sut.needUpdate(sourceFile, destDir)).isTrue();
  }

  @Test
  void needUpdate_whenDirectoryOnSourceAndFileOnDestination_true() throws IOException {
    // Given
    Path relativePath = fs.getPath("A");
    LocalFile sourceDir = createDirectory(source, relativePath);
    LocalFile destFile = createFile(destination, relativePath, "dest");

    // Then
    assertThat(sut.needUpdate(sourceDir, destFile)).isTrue();
  }

  @Test
  void needDelete_whenSameFileOnSourceAndDestination_false() throws IOException {
    // Given
    Path relativePath = fs.getPath("A");
    LocalFile sourceFile = createFile(source, relativePath, "source");
    LocalFile destFile = createFile(destination, relativePath, "source");

    // Then
    assertThat(sut.needDelete(Optional.of(sourceFile), destFile)).isFalse();
  }

  @Test
  void needDelete_whenDifferentFileOnSourceAndDestination_false() throws IOException {
    // Given
    Path relativePath = fs.getPath("A");
    LocalFile sourceFile = createFile(source, relativePath, "source");
    LocalFile destFile = createFile(destination, relativePath, "dest");

    // Then
    assertThat(sut.needDelete(Optional.of(sourceFile), destFile)).isFalse();
  }

  @Test
  void needDelete_whenDirectoryOnSourceAndDestination_false() throws IOException {
    // Given
    Path relativePath = fs.getPath("A/B/C");
    LocalFile sourceDir = createDirectory(source, relativePath);
    LocalFile destDir = createDirectory(destination, relativePath);

    // Then
    assertThat(sut.needDelete(Optional.of(sourceDir), destDir)).isFalse();
  }

  @Test
  void needDelete_whenFileOnlyOnDestination_true() throws IOException {
    // Given
    Path relativePath = fs.getPath("A");
    LocalFile destFile = createFile(destination, relativePath, "dest");

    // Then
    assertThat(sut.needDelete(Optional.empty(), destFile)).isTrue();
  }

  @Test
  void needDelete_whenFileOnSourceAndDirectoryOnDestination_true() throws IOException {
    // Given
    Path relativePath = fs.getPath("A");
    LocalFile sourceFile = createFile(source, relativePath, "source");
    LocalFile destDir = createDirectory(destination, relativePath);

    // Then
    assertThat(sut.needDelete(Optional.of(sourceFile), destDir)).isTrue();
  }

  @Test
  void needDelete_whenDirectoryOnSourceAndFileOnDestination_true() throws IOException {
    // Given
    Path relativePath = fs.getPath("A");
    LocalFile sourceDir = createDirectory(source, relativePath);
    LocalFile destFile = createFile(destination, relativePath, "dest");

    // Then
    assertThat(sut.needDelete(Optional.of(sourceDir), destFile)).isTrue();
  }

  @Test
  void toString_includesSourceAndDest() {
    assertThat(sut.toString())
        .isEqualTo("LocalBackup[source=LocalStorage[source], destination=LocalStorage[dest]]");
  }

  private StreamSubject assertThatFileSystem() throws IOException {
    try (Stream<Path> sourceFiles = Files.walk(sourceRoot).skip(1)) {
      try (Stream<Path> destFiles = Files.walk(destRoot).skip(1)) {
        return assertThat(Stream.concat(sourceFiles, destFiles).filter(LocalBackupTest::isLeaf));
      }
    }
  }

  private static boolean isLeaf(Path path) {
    if (Files.isRegularFile(path)) {
      return true;
    }
    try (Stream<Path> list = Files.list(path)) {
      return list.findAny().isEmpty();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static LocalFile createFile(LocalStorage localStorage, Path relativePath, String contents)
      throws IOException {
    Path path = localStorage.root().resolve(relativePath);
    MoreFiles.createParentDirectories(path);
    Files.createFile(path);
    Files.writeString(path, contents);
    // set fixed value, removing this variable from the unit tests here.
    Files.setLastModifiedTime(path, FileTime.fromMillis(0));
    return LocalFile.fromPath(localStorage, path);
  }

  private static LocalFile createDirectory(LocalStorage localStorage, Path relativePath)
      throws IOException {
    Path path = localStorage.root().resolve(relativePath);
    Files.createDirectories(path);
    return LocalFile.fromPath(localStorage, path);
  }
}
