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

  @BeforeEach
  void setUp() throws IOException {
    fs = Jimfs.newFileSystem(Configuration.forCurrentPlatform());
    sourceRoot = Files.createDirectory(fs.getPath("Documents"));
    destRoot = Files.createDirectory(fs.getPath("Backup"));
    sut = new LocalBackup(new LocalStorage(sourceRoot), new LocalStorage(destRoot));
  }

  @AfterEach
  void tearDown() throws IOException {
    fs.close();
  }

  @Test
  void createsFilesOnDestination() throws IOException {
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
    BackupRunner.OverallStatistics statistics = BackupRunner.run(sut);

    // Then
    assertThat(statistics)
        .isEqualTo(
            new BackupRunner.OverallStatistics(
                new BackupRunner.Statistics(4, 0, 0, 0, 45, 0),
                new BackupRunner.ErrorStatistics(0, 0, 0)));

    assertThat(Files.walk(sourceRoot))
        .containsExactly(
            sourceRoot,
            sourceRoot.resolve("A.txt"),
            sourceRoot.resolve("B"),
            sourceRoot.resolve("B/C"),
            sourceRoot.resolve("D"),
            sourceRoot.resolve("D/E.mp4"),
            sourceRoot.resolve("D/F.mp3"),
            sourceRoot.resolve("X"),
            sourceRoot.resolve("X/Y"),
            sourceRoot.resolve("X/Y/Z.pdf"));
    assertThat(Files.walk(destRoot))
        .containsExactly(
            destRoot,
            destRoot.resolve("A.txt"),
            destRoot.resolve("D"),
            destRoot.resolve("D/E.mp4"),
            destRoot.resolve("D/F.mp3"),
            destRoot.resolve("X"),
            destRoot.resolve("X/Y"),
            destRoot.resolve("X/Y/Z.pdf"));

    assertThat(Files.readString(sourceRoot.resolve("A.txt"))).isEqualTo("source text");
    assertThat(Files.readString(sourceRoot.resolve("D/E.mp4"))).isEqualTo("source video");
    assertThat(Files.readString(sourceRoot.resolve("D/F.mp3"))).isEqualTo("source audio");
    assertThat(Files.readString(sourceRoot.resolve("X/Y/Z.pdf"))).isEqualTo("source pdf");

    assertThat(Files.readString(destRoot.resolve("A.txt"))).isEqualTo("source text");
    assertThat(Files.readString(destRoot.resolve("D/E.mp4"))).isEqualTo("source video");
    assertThat(Files.readString(destRoot.resolve("D/F.mp3"))).isEqualTo("source audio");
    assertThat(Files.readString(destRoot.resolve("X/Y/Z.pdf"))).isEqualTo("source pdf");
  }

  @Test
  void updatesFilesOnDestination() throws IOException {
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
    BackupRunner.OverallStatistics statistics = BackupRunner.run(sut);

    // Then
    assertThat(statistics)
        .isEqualTo(
            new BackupRunner.OverallStatistics(
                new BackupRunner.Statistics(0, 4, 0, 0, 45, 37),
                new BackupRunner.ErrorStatistics(0, 0, 0)));

    assertThat(Files.walk(sourceRoot))
        .containsExactly(
            sourceRoot,
            sourceRoot.resolve("A.txt"),
            sourceRoot.resolve("B"),
            sourceRoot.resolve("B/C"),
            sourceRoot.resolve("D"),
            sourceRoot.resolve("D/E.mp4"),
            sourceRoot.resolve("D/F.mp3"),
            sourceRoot.resolve("X"),
            sourceRoot.resolve("X/Y"),
            sourceRoot.resolve("X/Y/Z.pdf"));
    assertThat(Files.walk(destRoot))
        .containsExactly(
            destRoot,
            destRoot.resolve("A.txt"),
            destRoot.resolve("B"),
            destRoot.resolve("B/C"),
            destRoot.resolve("D"),
            destRoot.resolve("D/E.mp4"),
            destRoot.resolve("D/F.mp3"),
            destRoot.resolve("X"),
            destRoot.resolve("X/Y"),
            destRoot.resolve("X/Y/Z.pdf"));

    assertThat(Files.readString(sourceRoot.resolve("A.txt"))).isEqualTo("source text");
    assertThat(Files.readString(sourceRoot.resolve("D/E.mp4"))).isEqualTo("source video");
    assertThat(Files.readString(sourceRoot.resolve("D/F.mp3"))).isEqualTo("source audio");
    assertThat(Files.readString(sourceRoot.resolve("X/Y/Z.pdf"))).isEqualTo("source pdf");

    assertThat(Files.readString(destRoot.resolve("A.txt"))).isEqualTo("source text");
    assertThat(Files.readString(destRoot.resolve("D/E.mp4"))).isEqualTo("source video");
    assertThat(Files.readString(destRoot.resolve("D/F.mp3"))).isEqualTo("source audio");
    assertThat(Files.readString(destRoot.resolve("X/Y/Z.pdf"))).isEqualTo("source pdf");
  }

  @Test
  void deletesFilesAndDirectoriesOnDestination() throws IOException {
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
    BackupRunner.OverallStatistics statistics = BackupRunner.run(sut);

    // Then
    assertThat(statistics)
        .isEqualTo(
            new BackupRunner.OverallStatistics(
                new BackupRunner.Statistics(0, 0, 4, 0, 0, 37),
                new BackupRunner.ErrorStatistics(0, 0, 0)));

    assertThat(Files.walk(sourceRoot)).containsExactly(sourceRoot);
    assertThat(Files.walk(destRoot)).containsExactly(destRoot);
  }

  @Test
  void overwritesDirectoryOnDestinationWithFileOnSource() throws IOException {
    // Given
    // bit of an edge case scenario... file matching dir name (i.e. no extension)
    createFile(sourceRoot.resolve("A/B/C"), "hello!");
    createDirectory(destRoot.resolve("A/B/C/D/E/F"));
    createDirectory(destRoot.resolve("A/B/C/X/Y/Z"));

    // When
    BackupRunner.OverallStatistics statistics = BackupRunner.run(sut);

    // Then
    assertThat(statistics)
        .isEqualTo(
            new BackupRunner.OverallStatistics(
                new BackupRunner.Statistics(1, 0, 0, 0, 6, 0),
                new BackupRunner.ErrorStatistics(0, 0, 0)));

    assertThat(Files.walk(sourceRoot))
        .containsExactly(
            sourceRoot,
            sourceRoot.resolve("A"),
            sourceRoot.resolve("A/B"),
            sourceRoot.resolve("A/B/C"));
    assertThat(Files.walk(destRoot))
        .containsExactly(
            destRoot, destRoot.resolve("A"), destRoot.resolve("A/B"), destRoot.resolve("A/B/C"));

    assertThat(Files.readString(sourceRoot.resolve("A/B/C"))).isEqualTo("hello!");
    assertThat(Files.readString(destRoot.resolve("A/B/C"))).isEqualTo("hello!");
  }

  private void createFile(Path path, String contents) throws IOException {
    Path parent = path.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    Files.createFile(path);
    Files.writeString(path, contents);
  }

  private void createDirectory(Path path) throws IOException {
    Files.createDirectories(path);
  }
}
