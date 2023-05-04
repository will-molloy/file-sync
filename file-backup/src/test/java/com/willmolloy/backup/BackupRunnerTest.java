package com.willmolloy.backup;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.willmolloy.backup.Backup.File;
import com.willmolloy.backup.Backup.Location;
import com.willmolloy.backup.BackupRunner.ErrorStatistics;
import com.willmolloy.backup.BackupRunner.OverallStatistics;
import com.willmolloy.backup.BackupRunner.Statistics;
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
  @Mock private Location mockDestination;
  @Mock private Backup<Location, Location> mockBackup;
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
    when(mockBackup.put(any())).thenReturn(true);

    // When
    OverallStatistics statistics = backupRunner.run();

    // Then
    verify(mockBackup).put("A");
    assertThat(statistics)
        .isEqualTo(new OverallStatistics(new Statistics(1, 0, 0), new ErrorStatistics(0, 0, 0)));
  }

  @Test
  void whenFileOnSourceAndDestination_andNotEqual_updatesFileOnDestination() {
    // Given
    when(mockSource.scan()).thenReturn(Map.of("A", new TestFile("ABC")));
    when(mockDestination.scan()).thenReturn(Map.of("A", new TestFile("XYZ")));
    when(mockBackup.put(any())).thenReturn(true);

    // When
    OverallStatistics statistics = backupRunner.run();

    // Then
    verify(mockBackup).put("A");
    assertThat(statistics)
        .isEqualTo(new OverallStatistics(new Statistics(0, 1, 0), new ErrorStatistics(0, 0, 0)));
  }

  @Test
  void whenFileOnSourceAndDestination_andEqual_skipsUpdate() {
    // Given
    when(mockSource.scan()).thenReturn(Map.of("A", new TestFile()));
    when(mockDestination.scan()).thenReturn(Map.of("A", new TestFile()));

    // When
    OverallStatistics statistics = backupRunner.run();

    // Then
    verify(mockBackup, never()).put("A");
    assertThat(statistics)
        .isEqualTo(new OverallStatistics(new Statistics(0, 0, 0), new ErrorStatistics(0, 0, 0)));
  }

  @Test
  void whenFileOnDestinationAndNotSource_deletesFileFromDestination() {
    // Given
    when(mockSource.scan()).thenReturn(Map.of());
    when(mockDestination.scan()).thenReturn(Map.of("A", new TestFile()));
    when(mockBackup.delete(any())).thenReturn(true);

    // When
    OverallStatistics statistics = backupRunner.run();

    // Then
    verify(mockBackup).delete("A");
    assertThat(statistics)
        .isEqualTo(new OverallStatistics(new Statistics(0, 0, 1), new ErrorStatistics(0, 0, 0)));
  }

  @Test
  void whenFileOnDestinationAndSource_skipsDelete() {
    // Given
    when(mockSource.scan()).thenReturn(Map.of("A", new TestFile()));
    when(mockDestination.scan()).thenReturn(Map.of("A", new TestFile()));

    // When
    OverallStatistics statistics = backupRunner.run();

    // Then
    verify(mockBackup, never()).delete("A");
    assertThat(statistics)
        .isEqualTo(new OverallStatistics(new Statistics(0, 0, 0), new ErrorStatistics(0, 0, 0)));
  }

  @Test
  void copyUpdateAndDelete() {
    // Given
    when(mockSource.scan())
        .thenReturn(
            Map.of(
                "A",
                new TestFile(),
                "B",
                new TestFile(),
                "C",
                new TestFile(),
                "D",
                new TestFile("Hello"),
                "E",
                new TestFile("world"),
                "F",
                new TestFile("!")));
    when(mockDestination.scan())
        .thenReturn(
            Map.of(
                "D",
                new TestFile(),
                "E",
                new TestFile(),
                "F",
                new TestFile(),
                "X",
                new TestFile(),
                "Y",
                new TestFile(),
                "Z",
                new TestFile()));
    when(mockBackup.put(any())).thenReturn(true);
    when(mockBackup.delete(any())).thenReturn(true);

    // When
    OverallStatistics statistics = backupRunner.run();

    // Then
    verify(mockBackup).put("A");
    verify(mockBackup).put("B");
    verify(mockBackup).put("C");
    verify(mockBackup).put("D");
    verify(mockBackup).put("E");
    verify(mockBackup).put("F");
    verify(mockBackup).delete("X");
    verify(mockBackup).delete("Y");
    verify(mockBackup).delete("Z");
    assertThat(statistics)
        .isEqualTo(new OverallStatistics(new Statistics(3, 3, 3), new ErrorStatistics(0, 0, 0)));
  }

  @Test
  void whenCopyFails_countsFailedCopy() {
    // Given
    when(mockSource.scan()).thenReturn(Map.of("A", new TestFile()));
    when(mockDestination.scan()).thenReturn(Map.of());
    when(mockBackup.put(any())).thenReturn(false);

    // When
    OverallStatistics statistics = backupRunner.run();

    // Then
    verify(mockBackup).put("A");
    assertThat(statistics)
        .isEqualTo(new OverallStatistics(new Statistics(0, 0, 0), new ErrorStatistics(1, 0, 0)));
  }

  @Test
  void whenUpdateFails_countsFailedUpdate() {
    // Given
    when(mockSource.scan()).thenReturn(Map.of("A", new TestFile("xyz")));
    when(mockDestination.scan()).thenReturn(Map.of("A", new TestFile("abc")));
    when(mockBackup.put(any())).thenReturn(false);

    // When
    OverallStatistics statistics = backupRunner.run();

    // Then
    verify(mockBackup).put("A");
    assertThat(statistics)
        .isEqualTo(new OverallStatistics(new Statistics(0, 0, 0), new ErrorStatistics(0, 1, 0)));
  }

  @Test
  void whenDeleteFails_countsFailedDelete() {
    // Given
    when(mockSource.scan()).thenReturn(Map.of());
    when(mockDestination.scan()).thenReturn(Map.of("A", new TestFile()));
    when(mockBackup.delete(any())).thenReturn(false);

    // When
    OverallStatistics statistics = backupRunner.run();

    // Then
    verify(mockBackup).delete("A");
    assertThat(statistics)
        .isEqualTo(new OverallStatistics(new Statistics(0, 0, 0), new ErrorStatistics(0, 0, 1)));
  }

  private record TestFile(String etag) implements File {
    private TestFile() {
      this("");
    }
  }
}
