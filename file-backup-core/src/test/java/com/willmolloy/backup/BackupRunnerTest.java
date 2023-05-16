package com.willmolloy.backup;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.willmolloy.backup.Backup.Location;
import com.willmolloy.backup.BackupRunner.ErrorStatistics;
import com.willmolloy.backup.BackupRunner.OverallStatistics;
import com.willmolloy.backup.BackupRunner.Statistics;
import com.willmolloy.backup.FileTree.File;
import java.nio.file.Path;
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

  @Mock private Location mockSource;
  @Mock private Location mockDest;
  @Mock private Backup<Location, Location> mockBackup;

  @BeforeEach
  void setUp() {
    when(mockBackup.source()).thenReturn(mockSource);
    when(mockBackup.destination()).thenReturn(mockDest);
  }

  @AfterEach
  void tearDown() {
    verify(mockSource).scan();
    verify(mockDest).scan();
    verifyNoMoreInteractions(mockSource);
    verifyNoMoreInteractions(mockDest);
    verifyNoMoreInteractions(mockBackup);
  }

  @Test
  void whenFileOnlyOnSource_createsFileOnDestination() {
    // Given
    when(mockSource.scan()).thenReturn(FileTree.create(Map.ofEntries(file("A"))));
    when(mockDest.scan()).thenReturn(emptyFileTree());
    when(mockBackup.put(any())).thenReturn(true);

    // When
    OverallStatistics statistics = BackupRunner.run(mockBackup);

    // Then
    verify(mockBackup).put(Path.of("A"));
    assertThat(statistics)
        .isEqualTo(
            new OverallStatistics(new Statistics(1, 0, 0, 0, 0, 0), new ErrorStatistics(0, 0, 0)));
  }

  @Test
  void whenFileOnSourceAndDestination_andNotSame_updatesFileOnDestination() {
    // Given
    var differentFile = file("A");
    when(differentFile.getValue().same(differentFile.getValue())).thenReturn(false);

    when(mockSource.scan()).thenReturn(FileTree.create(Map.ofEntries(differentFile)));
    when(mockDest.scan()).thenReturn(FileTree.create(Map.ofEntries(differentFile)));
    when(mockBackup.put(any())).thenReturn(true);

    // When
    OverallStatistics statistics = BackupRunner.run(mockBackup);

    // Then
    verify(mockBackup).put(Path.of("A"));
    assertThat(statistics)
        .isEqualTo(
            new OverallStatistics(new Statistics(0, 1, 0, 0, 0, 0), new ErrorStatistics(0, 0, 0)));
  }

  @Test
  void whenFileOnSourceAndDestination_andSame_skipsUpdate() {
    // Given
    var sameFile = file("A");
    when(sameFile.getValue().same(sameFile.getValue())).thenReturn(true);

    when(mockSource.scan()).thenReturn(FileTree.create(Map.ofEntries(sameFile)));
    when(mockDest.scan()).thenReturn(FileTree.create(Map.ofEntries(sameFile)));

    // When
    OverallStatistics statistics = BackupRunner.run(mockBackup);

    // Then
    verify(mockBackup, never()).put(Path.of("A"));
    assertThat(statistics)
        .isEqualTo(
            new OverallStatistics(new Statistics(0, 0, 0, 1, 0, 0), new ErrorStatistics(0, 0, 0)));
  }

  @Test
  void whenFileOnlyOnDestination_deletesFileFromDestination() {
    // Given
    when(mockSource.scan()).thenReturn(emptyFileTree());
    when(mockDest.scan()).thenReturn(FileTree.create(Map.ofEntries(file("A"))));
    when(mockBackup.delete(any())).thenReturn(true);

    // When
    OverallStatistics statistics = BackupRunner.run(mockBackup);

    // Then
    verify(mockBackup).delete(Path.of("A"));
    assertThat(statistics)
        .isEqualTo(
            new OverallStatistics(new Statistics(0, 0, 1, 0, 0, 0), new ErrorStatistics(0, 0, 0)));
  }

  @Test
  void whenFileOnDestinationAndSource_skipsDelete() {
    // Given
    var sameFile = file("A");
    when(sameFile.getValue().same(sameFile.getValue())).thenReturn(true);

    when(mockSource.scan()).thenReturn(FileTree.create(Map.ofEntries(sameFile)));
    when(mockDest.scan()).thenReturn(FileTree.create(Map.ofEntries(sameFile)));

    // When
    OverallStatistics statistics = BackupRunner.run(mockBackup);

    // Then
    verify(mockBackup, never()).delete(Path.of("A"));
    assertThat(statistics)
        .isEqualTo(
            new OverallStatistics(new Statistics(0, 0, 0, 1, 0, 0), new ErrorStatistics(0, 0, 0)));
  }

  @Test
  void whenCreateFails_countsFailedCreate() {
    // Given
    when(mockSource.scan()).thenReturn(FileTree.create(Map.ofEntries(file("A"))));
    when(mockDest.scan()).thenReturn(emptyFileTree());
    when(mockBackup.put(any())).thenReturn(false);

    // When
    OverallStatistics statistics = BackupRunner.run(mockBackup);

    // Then
    verify(mockBackup).put(Path.of("A"));
    assertThat(statistics)
        .isEqualTo(
            new OverallStatistics(new Statistics(0, 0, 0, 0, 0, 0), new ErrorStatistics(1, 0, 0)));
  }

  @Test
  void whenUpdateFails_countsFailedUpdate() {
    // Given
    when(mockSource.scan()).thenReturn(FileTree.create(Map.ofEntries(file("A"))));
    when(mockDest.scan()).thenReturn(FileTree.create(Map.ofEntries(file("A"))));
    when(mockBackup.put(any())).thenReturn(false);

    // When
    OverallStatistics statistics = BackupRunner.run(mockBackup);

    // Then
    verify(mockBackup).put(Path.of("A"));
    assertThat(statistics)
        .isEqualTo(
            new OverallStatistics(new Statistics(0, 0, 0, 0, 0, 0), new ErrorStatistics(0, 1, 0)));
  }

  @Test
  void whenDeleteFails_countsFailedDelete() {
    // Given
    when(mockSource.scan()).thenReturn(emptyFileTree());
    when(mockDest.scan()).thenReturn(FileTree.create(Map.ofEntries(file("A"))));
    when(mockBackup.delete(any())).thenReturn(false);

    // When
    OverallStatistics statistics = BackupRunner.run(mockBackup);

    // Then
    verify(mockBackup).delete(Path.of("A"));
    assertThat(statistics)
        .isEqualTo(
            new OverallStatistics(new Statistics(0, 0, 0, 0, 0, 0), new ErrorStatistics(0, 0, 1)));
  }

  @Test
  void whenPutCoveredByChild_skipsPut() {
    // Given
    when(mockSource.scan())
        .thenReturn(FileTree.create(Map.ofEntries(file("A"), file("A/B"), file("A/B/C"))));
    when(mockDest.scan()).thenReturn(emptyFileTree());
    when(mockBackup.put(any())).thenReturn(true);

    // When
    OverallStatistics statistics = BackupRunner.run(mockBackup);

    // Then
    verify(mockBackup, never()).put(Path.of("A"));
    verify(mockBackup, never()).put(Path.of("A/B"));
    verify(mockBackup).put(Path.of("A/B/C"));
    assertThat(statistics)
        .isEqualTo(
            new OverallStatistics(new Statistics(1, 0, 0, 0, 0, 0), new ErrorStatistics(0, 0, 0)));
  }

  @Test
  void whenDeleteCoveredByParent_skipsDelete() {
    // Given
    var root = file("A");
    when(root.getValue().same(root.getValue())).thenReturn(true);

    // cover 'parent NOT in source'
    when(mockSource.scan()).thenReturn(FileTree.create(Map.ofEntries(root)));
    when(mockDest.scan())
        .thenReturn(FileTree.create(Map.ofEntries(root, file("A/B"), file("A/B/C"))));
    when(mockBackup.delete(any())).thenReturn(true);

    // When
    OverallStatistics statistics = BackupRunner.run(mockBackup);

    // Then
    verify(mockBackup, never()).delete(Path.of("A"));
    verify(mockBackup).delete(Path.of("A/B"));
    verify(mockBackup, never()).delete(Path.of("A/B/C"));
    assertThat(statistics)
        .isEqualTo(
            new OverallStatistics(new Statistics(0, 0, 1, 1, 0, 0), new ErrorStatistics(0, 0, 0)));
  }

  @Test
  void countsAllLeavesPut() {
    // Given
    when(mockSource.scan())
        .thenReturn(
            FileTree.create(
                Map.ofEntries(
                    file("A"),
                    file("B"),
                    file("C"),
                    file("D"),
                    file("D/E"),
                    file("D/F"),
                    file("D/G"),
                    file("X"),
                    file("X/Y"),
                    file("X/Y/Z"))));
    when(mockDest.scan()).thenReturn(emptyFileTree());
    when(mockBackup.put(any())).thenReturn(true);

    // When
    OverallStatistics statistics = BackupRunner.run(mockBackup);

    // Then
    verify(mockBackup).put(Path.of("A"));
    verify(mockBackup).put(Path.of("B"));
    verify(mockBackup).put(Path.of("C"));
    verify(mockBackup, never()).put(Path.of("D"));
    verify(mockBackup).put(Path.of("D/E"));
    verify(mockBackup).put(Path.of("D/F"));
    verify(mockBackup).put(Path.of("D/G"));
    verify(mockBackup, never()).put(Path.of("X"));
    verify(mockBackup, never()).put(Path.of("X/Y"));
    verify(mockBackup).put(Path.of("X/Y/Z"));
    assertThat(statistics)
        .isEqualTo(
            new OverallStatistics(new Statistics(7, 0, 0, 0, 0, 0), new ErrorStatistics(0, 0, 0)));
  }

  @Test
  void countsAllLeavesDeleted() {
    // Given
    when(mockDest.scan())
        .thenReturn(
            FileTree.create(
                Map.ofEntries(
                    file("A"),
                    file("B"),
                    file("C"),
                    file("D"),
                    file("D/E"),
                    file("D/F"),
                    file("D/G"),
                    file("X"),
                    file("X/Y"),
                    file("X/Y/Z"))));
    when(mockSource.scan()).thenReturn(emptyFileTree());
    when(mockBackup.delete(any())).thenReturn(true);

    // When
    OverallStatistics statistics = BackupRunner.run(mockBackup);

    // Then
    verify(mockBackup).delete(Path.of("A"));
    verify(mockBackup).delete(Path.of("B"));
    verify(mockBackup).delete(Path.of("C"));
    verify(mockBackup).delete(Path.of("D"));
    verify(mockBackup, never()).delete(Path.of("D/E"));
    verify(mockBackup, never()).delete(Path.of("D/F"));
    verify(mockBackup, never()).delete(Path.of("D/G"));
    verify(mockBackup).delete(Path.of("X"));
    verify(mockBackup, never()).delete(Path.of("X/Y"));
    verify(mockBackup, never()).delete(Path.of("X/Y/Z"));
    assertThat(statistics)
        .isEqualTo(
            new OverallStatistics(new Statistics(0, 0, 7, 0, 0, 0), new ErrorStatistics(0, 0, 0)));
  }

  private static Map.Entry<Path, File> file(String path) {
    return Map.entry(Path.of(path), mock(File.class));
  }

  private static FileTree emptyFileTree() {
    return FileTree.create(Map.of());
  }
}
