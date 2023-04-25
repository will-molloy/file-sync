package com.willmolloy.backup;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * BackupAlgorithmTest. Ensures only the expected operations are run.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
@ExtendWith(MockitoExtension.class)
class BackupAlgorithmTest {

  @Mock private Backup mockBackup;
  @InjectMocks private BackupAlgorithm backupAlgorithm;

  @AfterEach
  void tearDown() {
    verify(mockBackup).scanSource();
    verify(mockBackup).scanDestination();
    verifyNoMoreInteractions(mockBackup);
  }

  @Test
  void whenFilesOnlyOnSource_copiesFilesToDestination() {
    // Given
    when(mockBackup.scanSource()).thenReturn(Stream.of(Path.of("A"), Path.of("B"), Path.of("C")));
    when(mockBackup.scanDestination()).thenReturn(Stream.of());

    // When
    backupAlgorithm.run();

    // Then
    verify(mockBackup).copy(Path.of("A"));
    verify(mockBackup).copy(Path.of("B"));
    verify(mockBackup).copy(Path.of("C"));
  }

  @Test
  void whenFilesOnlyOnDestination_deletesFilesFromDestination() {
    // Given
    when(mockBackup.scanSource()).thenReturn(Stream.of());
    when(mockBackup.scanDestination())
        .thenReturn(Stream.of(Path.of("D"), Path.of("E"), Path.of("F")));

    // When
    backupAlgorithm.run();

    // Then
    verify(mockBackup).delete(Path.of("D"));
    verify(mockBackup).delete(Path.of("E"));
    verify(mockBackup).delete(Path.of("F"));
  }

  @Test
  void whenFilesOnSourceAndDestination_updatesFilesOnDestination() {
    // Given
    when(mockBackup.scanSource()).thenReturn(Stream.of(Path.of("X"), Path.of("Y"), Path.of("Z")));
    when(mockBackup.scanDestination())
        .thenReturn(Stream.of(Path.of("X"), Path.of("Y"), Path.of("Z")));

    // When
    backupAlgorithm.run();

    // Then
    verify(mockBackup).update(Path.of("X"));
    verify(mockBackup).update(Path.of("Y"));
    verify(mockBackup).update(Path.of("Z"));
  }

  @Test
  void copyDeleteAndUpdate() {
    // Given
    when(mockBackup.scanSource())
        .thenReturn(
            Stream.of(
                Path.of("A"),
                Path.of("B"),
                Path.of("C"),
                Path.of("X"),
                Path.of("Y"),
                Path.of("Z")));
    when(mockBackup.scanDestination())
        .thenReturn(
            Stream.of(
                Path.of("D"),
                Path.of("E"),
                Path.of("F"),
                Path.of("X"),
                Path.of("Y"),
                Path.of("Z")));

    // When
    backupAlgorithm.run();

    // Then
    verify(mockBackup).copy(Path.of("A"));
    verify(mockBackup).copy(Path.of("B"));
    verify(mockBackup).copy(Path.of("C"));
    verify(mockBackup).delete(Path.of("D"));
    verify(mockBackup).delete(Path.of("E"));
    verify(mockBackup).delete(Path.of("F"));
    verify(mockBackup).update(Path.of("X"));
    verify(mockBackup).update(Path.of("Y"));
    verify(mockBackup).update(Path.of("Z"));
  }

  @Test
  void whenDirectoryOnlyOnSource_copiesDirectoryToDestination() {
    // Given
    when(mockBackup.scanSource())
        .thenReturn(Stream.of(Path.of("A"), Path.of("A/B"), Path.of("A/B/C")));
    when(mockBackup.scanDestination()).thenReturn(Stream.of());

    // When
    backupAlgorithm.run();

    // Then
    verify(mockBackup).copy(Path.of("A/B/C"));
  }

  @Test
  void whenDirectoryOnlyOnDestination_deletesDirectoryFromDestination() {
    // Given
    when(mockBackup.scanSource()).thenReturn(Stream.of());
    when(mockBackup.scanDestination())
        .thenReturn(Stream.of(Path.of("A"), Path.of("A/B"), Path.of("A/B/C")));

    // When
    backupAlgorithm.run();

    // Then
    verify(mockBackup).delete(Path.of("A"));
  }

  @Test
  void whenDirectoryOnSourceAndDestination_updatesDirectoryOnDestination() {
    // Given
    when(mockBackup.scanSource())
        .thenReturn(Stream.of(Path.of("A"), Path.of("A/B"), Path.of("A/B/C")));
    when(mockBackup.scanDestination())
        .thenReturn(Stream.of(Path.of("A"), Path.of("A/B"), Path.of("A/B/C")));

    // When
    backupAlgorithm.run();

    // Then
    verify(mockBackup).update(Path.of("A/B/C"));
  }

  @Test
  void whenDirectoryOnSourceAndParentDirectoryOnDestination_copiesChildDirectoryToDestination() {
    // Given
    when(mockBackup.scanSource())
        .thenReturn(Stream.of(Path.of("A"), Path.of("A/B"), Path.of("A/B/C")));
    when(mockBackup.scanDestination()).thenReturn(Stream.of(Path.of("A"), Path.of("A/B")));

    // When
    backupAlgorithm.run();

    // Then
    verify(mockBackup).copy(Path.of("A/B/C"));
  }

  @Test
  void whenDirectoryOnSourceAndChildDirectoryOnDestination_deletesChildDirectoryFromDestination() {
    // Given
    when(mockBackup.scanSource()).thenReturn(Stream.of(Path.of("A"), Path.of("A/B")));
    when(mockBackup.scanDestination())
        .thenReturn(Stream.of(Path.of("A"), Path.of("A/B"), Path.of("A/B/C")));

    // When
    backupAlgorithm.run();

    // Then
    verify(mockBackup).delete(Path.of("A/B/C"));
  }
}
