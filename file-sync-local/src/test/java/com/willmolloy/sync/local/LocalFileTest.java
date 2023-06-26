package com.willmolloy.sync.local;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.collect.Range;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * LocalFileTest.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
@SuppressFBWarnings("UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
class LocalFileTest {

  private FileSystem fs;
  private LocalStorage storage;

  @BeforeEach
  void setUp() throws IOException {
    fs = Jimfs.newFileSystem(Configuration.forCurrentPlatform());

    Path rootDir = fs.getPath("root");
    Files.createDirectory(rootDir);
    storage = new LocalStorage(rootDir);
  }

  @AfterEach
  void tearDown() throws IOException {
    fs.close();
  }

  @Test
  void uri_returnsFullPath() throws IOException {
    // Given
    Path path = Files.createFile(storage.root().resolve("A"));
    LocalFile file = LocalFile.fromPath(storage, path);

    // Then
    assertThat(file.uri()).isEqualTo(path.toString());
  }

  @Test
  void relativePath_returnsRelativizedPath() throws IOException {
    // Given
    Path path = Files.createFile(storage.root().resolve("A"));
    LocalFile file = LocalFile.fromPath(storage, path);

    // Then
    assertThat(file.relativePath()).isEqualTo(fs.getPath("A"));
  }

  @ParameterizedTest
  @MethodSource
  void size_returnsSizeOfFileInBytes(String contents, int size) throws IOException {
    // Given
    Path path = Files.createFile(storage.root().resolve("A"));
    Files.writeString(path, contents);
    LocalFile file = LocalFile.fromPath(storage, path);

    // When
    long result = file.size();

    // Then
    assertThat(result).isEqualTo(size);
  }

  static Stream<Arguments> size_returnsSizeOfFileInBytes() {
    return Stream.of(Arguments.of("", 0), Arguments.of("Hello world", 11));
  }

  @Test
  void lastModified_returnsLastModifiedTimeInMillis() throws IOException {
    // Given
    Path path = Files.createFile(storage.root().resolve("A"));
    LocalFile file = LocalFile.fromPath(storage, path);

    // When
    long result = file.lastModified();

    // Then
    long tolerance = 100;
    long currentMillis = System.currentTimeMillis();
    assertThat(result).isIn(Range.closed(currentMillis - tolerance, currentMillis + tolerance));
  }

  @Test
  void isDirectory_whenFile_false() throws IOException {
    // Given
    Path path = Files.createFile(storage.root().resolve("A"));
    LocalFile file = LocalFile.fromPath(storage, path);

    // Then
    assertThat(file.isDirectory()).isFalse();
  }

  @Test
  void isDirectory_whenDirectory_true() throws IOException {
    // Given
    Path path = Files.createDirectory(storage.root().resolve("A"));
    LocalFile file = LocalFile.fromPath(storage, path);

    // Then
    assertThat(file.isDirectory()).isTrue();
  }

  @Test
  void toString_includesUri() throws IOException {
    // Given
    Path path = Files.createFile(storage.root().resolve("ABCD"));
    LocalFile file = LocalFile.fromPath(storage, path);

    // Then
    assertThat(file.toString()).isEqualTo("LocalFile[%s]".formatted(path));
  }

  @Test
  void constructor_requiresObjectKeyBelowBucketPrefix() {
    // Given
    Path path = fs.getPath("A");

    // Then
    IllegalArgumentException thrown =
        assertThrows(
            IllegalArgumentException.class, () -> LocalFile.fromAttributes(storage, path, null));
    assertThat(thrown).hasMessageThat().isEqualTo("Requires path [A] to be under root [root]");
  }

  @Test
  void directoryFiller() {
    // Given
    LocalFile directoryFiller = LocalFile.directoryFiller(storage, "A/B/C");

    // Then
    Path relativePath = storage.root().getFileSystem().getPath("A/B/C");
    assertThat(directoryFiller.uri()).isEqualTo(storage.root().resolve(relativePath).toString());
    assertThat(directoryFiller.relativePath()).isEqualTo(relativePath);
    assertThat(directoryFiller.isDirectory()).isEqualTo(true);
    assertThat(directoryFiller.size()).isEqualTo(0);
    assertThat(directoryFiller.lastModified()).isEqualTo(0);
  }
}
