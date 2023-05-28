package com.willmolloy.backup.local;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.google.common.collect.Range;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.willmolloy.backup.File;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
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

  @ParameterizedTest
  @MethodSource
  void same_whenOtherIsLocalFile_onlyTrueWhenSizeAndLastModifiedEqual(
      String thisContents,
      long thisLastModified,
      String otherContents,
      long otherLastModified,
      boolean expected)
      throws IOException {
    // Given
    Path thisFilePath = Files.createFile(storage.root().resolve("A"));
    Files.writeString(thisFilePath, thisContents);
    Files.setLastModifiedTime(thisFilePath, FileTime.fromMillis(thisLastModified));
    LocalFile thisFile = spy(LocalFile.fromPath(storage, thisFilePath));

    Path otherFilePath = Files.createFile(storage.root().resolve("B"));
    Files.writeString(otherFilePath, otherContents);
    Files.setLastModifiedTime(otherFilePath, FileTime.fromMillis(otherLastModified));
    LocalFile otherFile = spy(LocalFile.fromPath(storage, otherFilePath));

    // Then
    assertThat(thisFile.same(otherFile)).isEqualTo(expected);
  }

  static Stream<Arguments> same_whenOtherIsLocalFile_onlyTrueWhenSizeAndLastModifiedEqual() {
    long currentMillis = System.currentTimeMillis();
    return Stream.of(
        Arguments.of("ABC", currentMillis, "ABC", currentMillis, true),
        Arguments.of("ABC", currentMillis, "XYZ", currentMillis, true),
        Arguments.of("ABC", currentMillis, "ABCD", currentMillis, false),
        Arguments.of("ABC", currentMillis, "ABC", currentMillis + 1, false),
        Arguments.of("ABC", currentMillis, "XYZ", currentMillis + 1, false),
        Arguments.of("ABC", currentMillis, "ABCD", currentMillis + 1, false));
  }

  @ParameterizedTest
  @MethodSource
  void same_whenOtherNotLocalFile_onlyTrueWhenSizeEqual(
      String thisContents, String otherContents, boolean expected) throws IOException {
    // Given
    Path thisFilePath = Files.createFile(storage.root().resolve("A"));
    Files.writeString(thisFilePath, thisContents);
    LocalFile thisFile = spy(LocalFile.fromPath(storage, thisFilePath));

    File otherFile = mock(File.class);
    when(otherFile.size()).thenReturn((long) otherContents.length());

    // Then
    assertThat(thisFile.same(otherFile)).isEqualTo(expected);
  }

  // TODO coverage for isDirectory() == other.isDirectory()???
  static Stream<Arguments> same_whenOtherNotLocalFile_onlyTrueWhenSizeEqual() {
    return Stream.of(
        Arguments.of("ABC", "ABC", true),
        Arguments.of("ABC", "XYZ", true),
        Arguments.of("ABC", "ABCD", false),
        Arguments.of("XYZ", "AB", false));
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
}
