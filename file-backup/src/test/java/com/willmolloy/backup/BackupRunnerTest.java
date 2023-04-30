package com.willmolloy.backup;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * BackupRunnerTest.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
@ExtendWith(MockitoExtension.class)
class BackupRunnerTest {

  @Mock private Backup.Location mockSource;
  @Mock private Backup.Location mockDestination;
  @Mock private Backup mockBackup;
  private BackupRunner backupRunner;

  @BeforeEach
  void setUp() {
    when(mockBackup.source()).thenReturn(mockSource);
    when(mockBackup.destination()).thenReturn(mockDestination);
    backupRunner = new BackupRunner(mockBackup);
  }

  @AfterEach
  void tearDown() {
    verify(mockSource).scan();
    verify(mockDestination).scan();
    verifyNoMoreInteractions(mockSource);
    verifyNoMoreInteractions(mockDestination);
    verifyNoMoreInteractions(mockBackup);
  }

  @Test
  void whenFileOnSourceAndNotDestination_copiesFileFromSourceToDestination() {
    // Given
    Path file = Path.of("A");
    when(mockSource.scan()).thenReturn(Stream.of(file));
    when(mockDestination.scan()).thenReturn(Stream.of());

    when(mockDestination.exists(file)).thenReturn(false);

    // When
    backupRunner.run();

    // Then
    verify(mockBackup).copy(file);
  }

  @Test
  void whenFileOnSourceAndDestination_andDifferentFileSize_updatesFileOnDestination() {
    // Given
    Path file = Path.of("A");
    when(mockSource.scan()).thenReturn(Stream.of(file));
    when(mockDestination.scan()).thenReturn(Stream.of());

    when(mockDestination.exists(file)).thenReturn(true);

    when(mockSource.isDirectory(file)).thenReturn(false);
    when(mockDestination.isDirectory(file)).thenReturn(false);

    when(mockSource.size(file)).thenReturn(1L);
    when(mockDestination.size(file)).thenReturn(2L);

    // When
    backupRunner.run();

    // Then
    verify(mockBackup).update(file);
  }

  @Test
  void whenFileOnSourceAndDestination_andDifferentModifiedTime_updatesFileOnDestination() {
    // Given
    Path file = Path.of("A");
    when(mockSource.scan()).thenReturn(Stream.of(file));
    when(mockDestination.scan()).thenReturn(Stream.of());

    when(mockDestination.exists(file)).thenReturn(true);

    when(mockSource.isDirectory(file)).thenReturn(false);
    when(mockDestination.isDirectory(file)).thenReturn(false);

    when(mockSource.size(file)).thenReturn(1L);
    when(mockDestination.size(file)).thenReturn(1L);

    when(mockSource.lastModified(file)).thenReturn(1L);
    when(mockDestination.lastModified(file)).thenReturn(2L);

    // When
    backupRunner.run();

    // Then
    verify(mockBackup).update(file);
  }

  @Test
  void whenFileOnSourceAndDestination_andEqual_skipsUpdate() {
    // Given
    Path file = Path.of("A");
    when(mockSource.scan()).thenReturn(Stream.of(file));
    when(mockDestination.scan()).thenReturn(Stream.of());

    when(mockDestination.exists(file)).thenReturn(true);

    when(mockSource.isDirectory(file)).thenReturn(false);
    when(mockDestination.isDirectory(file)).thenReturn(false);

    when(mockSource.size(file)).thenReturn(1L);
    when(mockDestination.size(file)).thenReturn(1L);

    when(mockSource.lastModified(file)).thenReturn(1L);
    when(mockDestination.lastModified(file)).thenReturn(1L);

    // When
    backupRunner.run();

    // Then
    verify(mockBackup, never()).update(file);
  }

  @Test
  void whenDirectoryOnSourceAndFileOnDestination_overwritesFileOnDestination() {
    // Given
    Path file = Path.of("A");
    when(mockSource.scan()).thenReturn(Stream.of(file));
    when(mockDestination.scan()).thenReturn(Stream.of());

    when(mockDestination.exists(file)).thenReturn(true);

    when(mockSource.isDirectory(file)).thenReturn(true);
    when(mockDestination.isDirectory(file)).thenReturn(false);

    // When
    backupRunner.run();

    // Then
    verify(mockBackup).delete(file);
    verify(mockBackup).copy(file);
  }

  @Test
  void whenFileOnSourceAndDirectoryOnDestination_overwritesDirectoryOnDestination() {
    // Given
    Path file = Path.of("A");
    when(mockSource.scan()).thenReturn(Stream.of(file));
    when(mockDestination.scan()).thenReturn(Stream.of());

    when(mockDestination.exists(file)).thenReturn(true);

    when(mockSource.isDirectory(file)).thenReturn(false);
    when(mockDestination.isDirectory(file)).thenReturn(true);

    // When
    backupRunner.run();

    // Then
    verify(mockBackup).delete(file);
    verify(mockBackup).copy(file);
  }

  @Test
  void whenDirectoryOnSourceAndDestination_skipsUpdate() {
    // Given
    Path file = Path.of("A");
    when(mockSource.scan()).thenReturn(Stream.of(file));
    when(mockDestination.scan()).thenReturn(Stream.of());

    when(mockDestination.exists(file)).thenReturn(true);

    when(mockSource.isDirectory(file)).thenReturn(true);
    when(mockDestination.isDirectory(file)).thenReturn(true);

    // When
    backupRunner.run();

    // Then
    verify(mockBackup, never()).update(file);
  }

  @Test
  void whenFileOnDestinationAndNotSource_deletesFileFromDestination() {
    // Given
    Path file = Path.of("A");
    when(mockSource.scan()).thenReturn(Stream.of());
    when(mockDestination.scan()).thenReturn(Stream.of(file));

    when(mockSource.exists(file)).thenReturn(false);

    // When
    backupRunner.run();

    // Then
    verify(mockBackup).delete(file);
  }

  @Test
  void whenFileOnDestinationAndSource_skipsDelete() {
    // Given
    Path file = Path.of("A");
    when(mockSource.scan()).thenReturn(Stream.of());
    when(mockDestination.scan()).thenReturn(Stream.of(file));

    when(mockSource.exists(file)).thenReturn(true);

    // When
    backupRunner.run();

    // Then
    verify(mockBackup, never()).delete(file);
  }
}
