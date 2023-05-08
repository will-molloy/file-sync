package com.willmolloy.backup.local;

import static com.google.common.truth.Truth.assertThat;
import static org.apache.commons.codec.digest.DigestUtils.md5Hex;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
  void etag_returnsMd5HashOfFileContentsInBase16(String contents, String md5Digest)
      throws IOException {
    // Given
    Path path = Files.createFile(root.resolve("A"));
    Files.writeString(path, contents);
    LocalFile file = new LocalFile(path);

    // When
    String result = file.etag();

    // Then
    assertThat(result).isEqualTo(md5Digest);
  }

  static Stream<Arguments> etag_returnsMd5HashOfFileContentsInBase16() {
    return Stream.of(
        Arguments.of("", "\"d41d8cd98f00b204e9800998ecf8427e\""),
        Arguments.of("Hello world", "\"3e25960a79dbc69b674cd4ec67a72c62\""));
  }

  @Test
  void etag_whenPathDoesntExist_failsGracefully() throws IOException {
    // Given
    LocalFile file = new LocalFile(Files.createFile(root.resolve("A")));
    Files.delete(file.path());

    // When
    String result = assertDoesNotThrow(() -> file.etag());

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  void constructor_requiresValidFile() {
    IllegalArgumentException thrown =
        assertThrows(
            IllegalArgumentException.class,
            () -> new LocalFile(Files.createDirectory(root.resolve("A"))));
    assertThat(thrown)
        .hasMessageThat()
        .isEqualTo("Requires a file: [%s]".formatted(root.resolve("A")));
  }

  @ParameterizedTest
  @MethodSource
  void sameContents_whenOtherIsLocalFile_ignoresETag_onlyTrueWhenSizeAndLastModifiedEqual(
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
    assertThat(thisFile.sameContents(otherFile)).isEqualTo(expected);
    verify(thisFile, never()).etag();
    verify(otherFile, never()).etag();
  }

  static Stream<Arguments>
      sameContents_whenOtherIsLocalFile_ignoresETag_onlyTrueWhenSizeAndLastModifiedEqual() {
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
  void sameContents_whenOtherNotLocalFile_onlyTrueWhenSizeAndETagEqual(
      String thisContents, String otherContents, boolean expected) throws IOException {
    // Given
    Path thisFilePath = Files.createFile(root.resolve("A"));
    Files.writeString(thisFilePath, thisContents);
    LocalFile thisFile = spy(new LocalFile(thisFilePath));

    Backup.File otherFile = mock(Backup.File.class);
    when(otherFile.etag()).thenReturn("\"%s\"".formatted(md5Hex(otherContents)));
    when(otherFile.size()).thenReturn((long) otherContents.length());

    // Then
    assertThat(thisFile.sameContents(otherFile)).isEqualTo(expected);
    boolean etagCalled = thisContents.length() == otherContents.length();
    verify(thisFile, etagCalled ? times(1) : never()).etag();
    verify(otherFile, etagCalled ? times(1) : never()).etag();
  }

  static Stream<Arguments> sameContents_whenOtherNotLocalFile_onlyTrueWhenSizeAndETagEqual() {
    return Stream.of(
        Arguments.of("ABC", "ABC", true),
        Arguments.of("ABC", "XYZ", false),
        Arguments.of("ABC", "ABCD", false));
  }

  @Test
  void toString_includesPath() throws IOException {
    Path path = Files.createFile(root.resolve("ABCD"));
    LocalFile file = new LocalFile(path);
    assertThat(file.toString()).isEqualTo("LocalFile[%s]".formatted(path));
  }
}
