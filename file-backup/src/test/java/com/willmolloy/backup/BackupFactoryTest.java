package com.willmolloy.backup;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.willmolloy.backup.local.LocalBackup;
import com.willmolloy.backup.s3.S3Backup;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * BackupFactoryTest.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
class BackupFactoryTest {

  @Test
  void create_checksBackupType() {
    IllegalArgumentException thrown =
        assertThrows(IllegalArgumentException.class, () -> BackupFactory.create("AbcBackupXYZ"));
    assertThat(thrown).hasMessageThat().isEqualTo("Unknown backup type: AbcBackupXYZ");
  }

  @Test
  void create_checksArgLength() {
    IllegalArgumentException thrown =
        assertThrows(IllegalArgumentException.class, BackupFactory::create);
    assertThat(thrown).hasMessageThat().isEqualTo("Expected at least 1 arg");
  }

  @Test
  void create_whenLocalBackupType_createsLocalBackup() {
    // Given
    Backup<?, ?> backup =
        BackupFactory.create(LocalBackup.class.getSimpleName(), "source/root", "dest/root");

    // Then
    assertThat(backup).isInstanceOf(LocalBackup.class);
    LocalBackup localBackup = (LocalBackup) backup;
    assertThat(localBackup.source().root()).isEqualTo(Path.of("source/root"));
    assertThat(localBackup.destination().root()).isEqualTo(Path.of("dest/root"));
  }

  @Test
  void create_whenLocalBackupType_checksArgLength() {
    IllegalArgumentException thrown =
        assertThrows(
            IllegalArgumentException.class,
            () -> BackupFactory.create(LocalBackup.class.getSimpleName()));
    assertThat(thrown).hasMessageThat().isEqualTo("Expected 3 args: [LocalBackup]");
  }

  @Test
  void create_whenS3BackupType_createsS3Backup() {
    // Given
    Backup<?, ?> backup =
        BackupFactory.create(
            S3Backup.class.getSimpleName(), "source/root", "my-bucket", "backups/");

    // Then
    assertThat(backup).isInstanceOf(S3Backup.class);
    S3Backup s3Backup = (S3Backup) backup;
    assertThat(s3Backup.source().root()).isEqualTo(Path.of("source/root"));
    assertThat(s3Backup.destination().bucketName()).isEqualTo("my-bucket");
    assertThat(s3Backup.destination().prefix()).isEqualTo("backups/");
  }

  @Test
  void create_whenS3BackupType_checksArgLength() {
    IllegalArgumentException thrown =
        assertThrows(
            IllegalArgumentException.class,
            () -> BackupFactory.create(S3Backup.class.getSimpleName()));
    assertThat(thrown).hasMessageThat().isEqualTo("Expected 4 args: [S3Backup]");
  }
}
