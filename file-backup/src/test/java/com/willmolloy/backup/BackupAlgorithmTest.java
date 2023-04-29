package com.willmolloy.backup;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.Map;
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
    when(mockBackup.scanSource())
        .thenReturn(
            new FileTree(
                Map.of(
                    Path.of("A"), new FileTree.Node(),
                    Path.of("B"), new FileTree.Node(),
                    Path.of("C"), new FileTree.Node())));
    when(mockBackup.scanDestination()).thenReturn(new FileTree());

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
    when(mockBackup.scanSource()).thenReturn(new FileTree());
    when(mockBackup.scanDestination())
        .thenReturn(
            new FileTree(
                Map.of(
                    Path.of("D"), new FileTree.Node(),
                    Path.of("E"), new FileTree.Node(),
                    Path.of("F"), new FileTree.Node())));

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
    when(mockBackup.scanSource())
        .thenReturn(
            new FileTree(
                Map.of(
                    Path.of("X"), new FileTree.Node(),
                    Path.of("Y"), new FileTree.Node(),
                    Path.of("Z"), new FileTree.Node())));
    when(mockBackup.scanDestination())
        .thenReturn(
            new FileTree(
                Map.of(
                    Path.of("X"), new FileTree.Node(),
                    Path.of("Y"), new FileTree.Node(),
                    Path.of("Z"), new FileTree.Node())));

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
            new FileTree(
                Map.of(
                    Path.of("A"), new FileTree.Node(),
                    Path.of("B"), new FileTree.Node(),
                    Path.of("C"), new FileTree.Node(),
                    Path.of("X"), new FileTree.Node(),
                    Path.of("Y"), new FileTree.Node(),
                    Path.of("Z"), new FileTree.Node())));
    when(mockBackup.scanDestination())
        .thenReturn(
            new FileTree(
                Map.of(
                    Path.of("D"), new FileTree.Node(),
                    Path.of("E"), new FileTree.Node(),
                    Path.of("F"), new FileTree.Node(),
                    Path.of("X"), new FileTree.Node(),
                    Path.of("Y"), new FileTree.Node(),
                    Path.of("Z"), new FileTree.Node())));

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
        .thenReturn(
            new FileTree(
                Map.of(
                    Path.of("A"),
                    new FileTree.Node(
                        Map.of(
                            Path.of("A/B"),
                            new FileTree.Node(Map.of(Path.of("A/B/C"), new FileTree.Node())))))));
    when(mockBackup.scanDestination()).thenReturn(new FileTree());

    // When
    backupAlgorithm.run();

    // Then
    verify(mockBackup).copy(Path.of("A/B/C"));
  }

  @Test
  void whenDirectoryOnlyOnDestination_deletesDirectoryFromDestination() {
    // Given
    when(mockBackup.scanSource()).thenReturn(new FileTree());
    when(mockBackup.scanDestination())
        .thenReturn(
            new FileTree(
                Map.of(
                    Path.of("A"),
                    new FileTree.Node(
                        Map.of(
                            Path.of("A/B"),
                            new FileTree.Node(Map.of(Path.of("A/B/C"), new FileTree.Node())))))));

    // When
    backupAlgorithm.run();

    // Then
    verify(mockBackup).delete(Path.of("A"));
  }

  @Test
  void whenDirectoryOnSourceAndDestination_updatesDirectoryOnDestination() {
    // Given
    when(mockBackup.scanSource())
        .thenReturn(
            new FileTree(
                Map.of(
                    Path.of("A"),
                    new FileTree.Node(
                        Map.of(
                            Path.of("A/B"),
                            new FileTree.Node(Map.of(Path.of("A/B/C"), new FileTree.Node())))))));
    when(mockBackup.scanDestination())
        .thenReturn(
            new FileTree(
                Map.of(
                    Path.of("A"),
                    new FileTree.Node(
                        Map.of(
                            Path.of("A/B"),
                            new FileTree.Node(Map.of(Path.of("A/B/C"), new FileTree.Node())))))));

    // When
    backupAlgorithm.run();

    // Then
    verify(mockBackup).update(Path.of("A/B/C"));
  }

  @Test
  void whenDirectoryOnSourceAndParentDirectoryOnDestination_copiesChildDirectoryToDestination() {
    // Given
    when(mockBackup.scanSource())
        .thenReturn(
            new FileTree(
                Map.of(
                    Path.of("A"),
                    new FileTree.Node(
                        Map.of(
                            Path.of("A/B"),
                            new FileTree.Node(Map.of(Path.of("A/B/C"), new FileTree.Node())))))));
    when(mockBackup.scanDestination())
        .thenReturn(
            new FileTree(
                Map.of(
                    Path.of("A"), new FileTree.Node(Map.of(Path.of("A/B"), new FileTree.Node())))));

    // When
    backupAlgorithm.run();

    // Then
    verify(mockBackup).copy(Path.of("A/B/C"));
  }

  @Test
  void whenDirectoryOnSourceAndChildDirectoryOnDestination_deletesChildDirectoryFromDestination() {
    // Given
    when(mockBackup.scanSource())
        .thenReturn(
            new FileTree(
                Map.of(
                    Path.of("A"), new FileTree.Node(Map.of(Path.of("A/B"), new FileTree.Node())))));
    when(mockBackup.scanDestination())
        .thenReturn(
            new FileTree(
                Map.of(
                    Path.of("A"),
                    new FileTree.Node(
                        Map.of(
                            Path.of("A/B"),
                            new FileTree.Node(Map.of(Path.of("A/B/C"), new FileTree.Node())))))));

    // When
    backupAlgorithm.run();

    // Then
    verify(mockBackup).delete(Path.of("A/B/C"));
  }
}
