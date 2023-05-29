package com.willmolloy.backup.local;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.willmolloy.backup.BackupRunner;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * LocalBackupIntegrationTest.
 *
 * <p>Quite simple as most logic is covered by unit tests.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
class LocalBackupIntegrationTest {

  private FileSystem fs;
  private Path sourceRoot;
  private Path destRoot;
  private LocalBackup sut;

  // set fixed value, otherwise tests are flaky
  private final Instant lastModified = Instant.now();

  @BeforeEach
  void setUp() throws IOException {
    fs = Jimfs.newFileSystem(Configuration.forCurrentPlatform());
    sourceRoot = createDirectory(fs.getPath("Documents"));
    destRoot = createDirectory(fs.getPath("Backup"));
    sut = new LocalBackup(new LocalStorage(sourceRoot), new LocalStorage(destRoot));
  }

  @AfterEach
  void tearDown() throws IOException {
    fs.close();
  }

  @Test
  void whenFilesOnlyOnSource_createsFilesAndDirectoriesOnDestination() throws IOException {
    // Given
    // simple file
    createFile(sourceRoot.resolve("A.txt"), "source text");
    // empty directory
    createDirectory(sourceRoot.resolve("B/C"));
    // directory with multiple files
    createFile(sourceRoot.resolve("D/E.mp4"), "source video");
    createFile(sourceRoot.resolve("D/F.mp3"), "source audio");
    // file nested deep
    createFile(sourceRoot.resolve("X/Y/Z.pdf"), "source pdf");

    // When
    boolean result = BackupRunner.run(sut);

    // Then
    assertThat(result).isTrue();

    for (Path root : List.of(sourceRoot, destRoot)) {
      assertThat(Files.walk(root))
          .containsExactly(
              root,
              root.resolve("A.txt"),
              root.resolve("B"),
              root.resolve("B/C"),
              root.resolve("D"),
              root.resolve("D/E.mp4"),
              root.resolve("D/F.mp3"),
              root.resolve("X"),
              root.resolve("X/Y"),
              root.resolve("X/Y/Z.pdf"));
      assertThat(Files.readString(root.resolve("A.txt"))).isEqualTo("source text");
      assertThat(Files.readString(root.resolve("D/E.mp4"))).isEqualTo("source video");
      assertThat(Files.readString(root.resolve("D/F.mp3"))).isEqualTo("source audio");
      assertThat(Files.readString(root.resolve("X/Y/Z.pdf"))).isEqualTo("source pdf");
    }
  }

  @Test
  void whenFilesOnSourceAndDestination_updatesFilesAndDirectoriesOnDestination()
      throws IOException {
    // Given
    // simple file
    createFile(sourceRoot.resolve("A.txt"), "source text");
    // empty directory
    createDirectory(sourceRoot.resolve("B/C"));
    // directory with multiple files
    createFile(sourceRoot.resolve("D/E.mp4"), "source video");
    createFile(sourceRoot.resolve("D/F.mp3"), "source audio");
    // file nested deep
    createFile(sourceRoot.resolve("X/Y/Z.pdf"), "source pdf");

    // simple file
    createFile(destRoot.resolve("A.txt"), "dest text");
    // empty directory
    createDirectory(destRoot.resolve("B/C"));
    // directory with multiple files
    createFile(destRoot.resolve("D/E.mp4"), "dest video");
    createFile(destRoot.resolve("D/F.mp3"), "dest audio");
    // file nested deep
    createFile(destRoot.resolve("X/Y/Z.pdf"), "dest pdf");

    // When
    boolean result = BackupRunner.run(sut);

    // Then
    assertThat(result).isTrue();

    for (Path root : List.of(sourceRoot, destRoot)) {
      assertThat(Files.walk(root))
          .containsExactly(
              root,
              root.resolve("A.txt"),
              root.resolve("B"),
              root.resolve("B/C"),
              root.resolve("D"),
              root.resolve("D/E.mp4"),
              root.resolve("D/F.mp3"),
              root.resolve("X"),
              root.resolve("X/Y"),
              root.resolve("X/Y/Z.pdf"));
      assertThat(Files.readString(root.resolve("A.txt"))).isEqualTo("source text");
      assertThat(Files.readString(root.resolve("D/E.mp4"))).isEqualTo("source video");
      assertThat(Files.readString(root.resolve("D/F.mp3"))).isEqualTo("source audio");
      assertThat(Files.readString(root.resolve("X/Y/Z.pdf"))).isEqualTo("source pdf");
    }
  }

  @Test
  void whenFilesOnlyOnDestination_deletesFilesAndDirectoriesOnDestination() throws IOException {
    // Given
    // simple file
    createFile(destRoot.resolve("A.txt"), "dest text");
    // empty directory
    createDirectory(destRoot.resolve("B/C"));
    // directory with multiple files
    createFile(destRoot.resolve("D/E.mp4"), "dest video");
    createFile(destRoot.resolve("D/F.mp3"), "dest audio");
    // file nested deep
    createFile(destRoot.resolve("X/Y/Z.pdf"), "dest pdf");

    // When
    boolean result = BackupRunner.run(sut);

    // Then
    assertThat(result).isTrue();

    for (Path root : List.of(sourceRoot, destRoot)) {
      assertThat(Files.walk(root)).containsExactly(root);
    }
  }

  @Test
  void whenFileOnSourceAndNonEmptyDirectoryOnDestination_overwritesDirectoryOnDestination()
      throws IOException {
    // Given
    // bit of an edge case scenario... file matching dir name (i.e. no extension)
    createFile(sourceRoot.resolve("A/B/C"), "hello!");
    createFile(destRoot.resolve("A/B/C/D/E/F.txt"), "Hello");
    createFile(destRoot.resolve("A/B/C/X/Y/Z.pdf"), "World.");

    // When
    boolean result = BackupRunner.run(sut);

    // Then
    assertThat(result).isTrue();

    for (Path root : List.of(sourceRoot, destRoot)) {
      assertThat(Files.walk(root))
          .containsExactly(root, root.resolve("A"), root.resolve("A/B"), root.resolve("A/B/C"));
      assertThat(Files.readString(root.resolve("A/B/C"))).isEqualTo("hello!");
    }
  }

  @Test
  void whenNonEmptyDirectoryOnSourceAndFileOnDestination_overwritesFileOnDestination()
      throws IOException {
    // Given
    // TODO bug where 2nd worker tries delete after first worker copied...
    createFile(sourceRoot.resolve("A/B/C/D/E/F.txt"), "Hello");
    createFile(sourceRoot.resolve("A/B/C/X/Y/Z.pdf"), "World.");
    createFile(destRoot.resolve("A/B/C"), "hello!");

    // When
    boolean result = BackupRunner.run(sut);

    // Then
    assertThat(result).isTrue();

    for (Path root : List.of(sourceRoot, destRoot)) {
      assertThat(Files.walk(root))
          .containsExactly(
              root,
              root.resolve("A"),
              root.resolve("A/B"),
              root.resolve("A/B/C"),
              root.resolve("A/B/C/D"),
              root.resolve("A/B/C/D/E"),
              root.resolve("A/B/C/D/E/F.txt"),
              root.resolve("A/B/C/X"),
              root.resolve("A/B/C/X/Y"),
              root.resolve("A/B/C/X/Y/Z.pdf"));
      assertThat(Files.readString(root.resolve("A/B/C/D/E/F.txt"))).isEqualTo("Hello");
      assertThat(Files.readString(root.resolve("A/B/C/X/Y/Z.pdf"))).isEqualTo("World.");
    }
  }

  @Test
  void whenChildDirectoryOnlyOnSource_createsDirectoriesOnDestination() throws IOException {
    // Given
    createDirectory(sourceRoot.resolve("A/B/C/X/Y/Z"));
    createDirectory(destRoot.resolve("A/B/C"));

    // When
    boolean result = BackupRunner.run(sut);

    // Then
    assertThat(result).isTrue();

    for (Path root : List.of(sourceRoot, destRoot)) {
      assertThat(Files.walk(root))
          .containsExactly(
              root,
              root.resolve("A"),
              root.resolve("A/B"),
              root.resolve("A/B/C"),
              root.resolve("A/B/C/X"),
              root.resolve("A/B/C/X/Y"),
              root.resolve("A/B/C/X/Y/Z"));
    }
  }

  @Test
  void whenChildDirectoryOnlyOnDestination_deletesDirectoriesOnDestination() throws IOException {
    // Given
    createDirectory(sourceRoot.resolve("A/B/C"));
    createDirectory(destRoot.resolve("A/B/C/X/Y/Z"));

    // When
    boolean result = BackupRunner.run(sut);

    // Then
    assertThat(result).isTrue();

    for (Path root : List.of(sourceRoot, destRoot)) {
      assertThat(Files.walk(root))
          .containsExactly(root, root.resolve("A"), root.resolve("A/B"), root.resolve("A/B/C"));
    }
  }

  private void createFile(Path path, String contents) throws IOException {
    Path parent = path.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    Files.createFile(path);
    Files.writeString(path, contents);
    Files.setLastModifiedTime(path, FileTime.from(lastModified));
  }

  private Path createDirectory(Path path) throws IOException {
    Files.createDirectories(path);
    Files.setLastModifiedTime(path, FileTime.from(lastModified));
    return path;
  }
}
