package com.willmolloy.backup;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.willmolloy.backup.local.LocalBackup;
import com.willmolloy.backup.s3.S3Backup;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * BackupFactoryTest.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
class BackupFactoryTest {

  private FileSystem fs;
  private BackupFactory sut;

  @BeforeEach
  void setUp() {
    fs = Jimfs.newFileSystem(Configuration.forCurrentPlatform());
    sut = new BackupFactory(fs);
  }

  @AfterEach
  void tearDown() throws IOException {
    fs.close();
  }

  @Test
  void create_requiresValidBackupType() {
    IllegalArgumentException thrown =
        assertThrows(IllegalArgumentException.class, () -> sut.create("AbcBackupXYZ"));
    assertThat(thrown).hasMessageThat().isEqualTo("Unknown backup type: AbcBackupXYZ");
  }

  @Test
  void create_requiresAtLeast1Arg() {
    IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, sut::create);
    assertThat(thrown).hasMessageThat().isEqualTo("Requires at least 1 arg");
  }

  @Test
  void create_whenLocalBackupType_createsLocalBackup() throws IOException {
    // Given
    Path sourceRoot = Files.createDirectories(fs.getPath("source/root"));
    Path destRoot = Files.createDirectories(fs.getPath("dest/root"));
    Backup<?, ?> backup =
        sut.create(LocalBackup.class.getSimpleName(), sourceRoot.toString(), destRoot.toString());

    // Then
    assertThat(backup).isInstanceOf(LocalBackup.class);
    LocalBackup localBackup = (LocalBackup) backup;
    assertThat(localBackup.source().root()).isEqualTo(sourceRoot);
    assertThat(localBackup.destination().root()).isEqualTo(destRoot);
  }

  @Test
  void create_whenLocalBackupType_requires3Args() {
    IllegalArgumentException thrown =
        assertThrows(
            IllegalArgumentException.class, () -> sut.create(LocalBackup.class.getSimpleName()));
    assertThat(thrown).hasMessageThat().isEqualTo("Requires 3 args: [LocalBackup]");
  }

  @Test
  void create_whenS3BackupType_createsS3Backup() throws IOException {
    // Given
    Path sourceRoot = Files.createDirectories(fs.getPath("source/root"));
    Backup<?, ?> backup =
        sut.create(S3Backup.class.getSimpleName(), sourceRoot.toString(), "my-bucket", "backups/");

    // Then
    assertThat(backup).isInstanceOf(S3Backup.class);
    S3Backup s3Backup = (S3Backup) backup;
    assertThat(s3Backup.source().root()).isEqualTo(sourceRoot);
    assertThat(s3Backup.destination().bucketName()).isEqualTo("my-bucket");
    assertThat(s3Backup.destination().prefix()).isEqualTo("backups/");
  }

  @Test
  void create_whenS3BackupType_requires4Args() {
    IllegalArgumentException thrown =
        assertThrows(
            IllegalArgumentException.class, () -> sut.create(S3Backup.class.getSimpleName()));
    assertThat(thrown).hasMessageThat().isEqualTo("Requires 4 args: [S3Backup]");
  }
}
