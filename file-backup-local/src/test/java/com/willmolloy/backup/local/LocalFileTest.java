package com.willmolloy.backup.local;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.google.common.collect.Range;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.willmolloy.backup.Backup;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
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
  private Path root;

  @BeforeEach
  void setUp() throws IOException {
    fs = Jimfs.newFileSystem(Configuration.forCurrentPlatform());

    root = fs.getPath("root");
    Files.createDirectory(root);
  }

  @AfterEach
  void tearDown() throws IOException {
    fs.close();
  }

  @ParameterizedTest
  @MethodSource
  void size_returnsSizeOfFileInBytes(String contents, int size) throws IOException {
    // Given
    Path path = Files.createFile(root.resolve("A"));
    Files.writeString(path, contents);
    LocalFile file = new LocalFile(path);

    // When
    long result = file.size();

    // Then
    assertThat(result).isEqualTo(size);
  }

  static Stream<Arguments> size_returnsSizeOfFileInBytes() {
    return Stream.of(Arguments.of("", 0), Arguments.of("Hello world", 11));
  }

  @Test
  void lastModified_returnsLastModifiedTime() throws IOException {
    // Given
    LocalFile file = new LocalFile(Files.createFile(root.resolve("A")));

    // When
    Instant result = file.lastModified();

    // Then
    long tolerance = 100;
    long currentMillis = System.currentTimeMillis();
    assertThat(result.toEpochMilli())
        .isIn(Range.closed(currentMillis - tolerance, currentMillis + tolerance));
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
    Path thisFilePath = Files.createFile(root.resolve("A"));
    Files.writeString(thisFilePath, thisContents);
    Files.setLastModifiedTime(thisFilePath, FileTime.fromMillis(thisLastModified));
    LocalFile thisFile = spy(new LocalFile(thisFilePath));

    Path otherFilePath = Files.createFile(root.resolve("B"));
    Files.writeString(otherFilePath, otherContents);
    Files.setLastModifiedTime(otherFilePath, FileTime.fromMillis(otherLastModified));
    LocalFile otherFile = spy(new LocalFile(otherFilePath));

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
    Path thisFilePath = Files.createFile(root.resolve("A"));
    Files.writeString(thisFilePath, thisContents);
    LocalFile thisFile = spy(new LocalFile(thisFilePath));

    Backup.File otherFile = mock(Backup.File.class);
    when(otherFile.size()).thenReturn((long) otherContents.length());

    // Then
    assertThat(thisFile.same(otherFile)).isEqualTo(expected);
  }

  static Stream<Arguments> same_whenOtherNotLocalFile_onlyTrueWhenSizeEqual() {
    return Stream.of(
        Arguments.of("ABC", "ABC", true),
        Arguments.of("ABC", "XYZ", true),
        Arguments.of("ABC", "ABCD", false),
        Arguments.of("XYZ", "AB", false));
  }

  @Test
  void toString_includesPath() throws IOException {
    Path path = Files.createFile(root.resolve("ABCD"));
    LocalFile file = new LocalFile(path);
    assertThat(file.toString()).isEqualTo("LocalFile[%s]".formatted(path));
  }
}
