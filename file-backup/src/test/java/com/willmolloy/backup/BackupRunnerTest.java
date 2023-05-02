package com.willmolloy.backup;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
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
    when(mockSource.scan()).thenReturn(Map.of("A", new TestFile()));
    when(mockDestination.scan()).thenReturn(Map.of());

    // When
    backupRunner.run();

    // Then
    verify(mockBackup).copy("A");
  }

  @Test
  void whenFileOnSourceAndDestination_andDifferentSize_updatesFileOnDestination() {
    // Given
    when(mockSource.scan()).thenReturn(Map.of("A", new TestFile(2, Instant.ofEpochSecond(1))));
    when(mockDestination.scan()).thenReturn(Map.of("A", new TestFile(1, Instant.ofEpochSecond(1))));

    // When
    backupRunner.run();

    // Then
    verify(mockBackup).update("A");
  }

  @Test
  void whenFileOnSourceAndDestination_andDifferentModifiedTime_updatesFileOnDestination() {
    // Given
    when(mockSource.scan()).thenReturn(Map.of("A", new TestFile(2, Instant.ofEpochSecond(2))));
    when(mockDestination.scan()).thenReturn(Map.of("A", new TestFile(2, Instant.ofEpochSecond(1))));

    // When
    backupRunner.run();

    // Then
    verify(mockBackup).update("A");
  }

  @Test
  void whenFileOnSourceAndDestination_andEqual_skipsUpdate() {
    // Given
    when(mockSource.scan()).thenReturn(Map.of("A", new TestFile(2, Instant.ofEpochSecond(2))));
    when(mockDestination.scan()).thenReturn(Map.of("A", new TestFile(2, Instant.ofEpochSecond(2))));

    // When
    backupRunner.run();

    // Then
    verify(mockBackup, never()).update("A");
  }

  @Test
  void whenFileOnDestinationAndNotSource_deletesFileFromDestination() {
    // Given
    when(mockSource.scan()).thenReturn(Map.of());
    when(mockDestination.scan()).thenReturn(Map.of("A", new TestFile()));

    // When
    backupRunner.run();

    // Then
    verify(mockBackup).delete("A");
  }

  @Test
  void whenFileOnDestinationAndSource_skipsDelete() {
    // Given
    when(mockSource.scan()).thenReturn(Map.of("A", new TestFile()));
    when(mockDestination.scan()).thenReturn(Map.of("A", new TestFile()));

    // When
    backupRunner.run();

    // Then
    verify(mockBackup, never()).delete("A");
  }

  private record TestFile(long size, Instant lastModified) implements Backup.File {
    private TestFile() {
      this(0, Instant.MIN);
    }
  }
}
