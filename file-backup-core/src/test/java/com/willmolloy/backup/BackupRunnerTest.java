package com.willmolloy.backup;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.willmolloy.backup.Backup.Location;
import com.willmolloy.backup.Backup.Node;
import com.willmolloy.backup.BackupRunner.ErrorStatistics;
import com.willmolloy.backup.BackupRunner.OverallStatistics;
import com.willmolloy.backup.BackupRunner.Statistics;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

  @BeforeEach
  void setUp() {
    when(mockBackup.source()).thenReturn(mockSource);
    when(mockBackup.destination()).thenReturn(mockDestination);
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
  void whenFileOnSourceAndNotDestination_createsFileOnDestination() {
    // Given
    when(mockSource.scan()).thenReturn(Map.of("A", file()));
    when(mockDestination.scan()).thenReturn(Map.of());
    when(mockBackup.put(any())).thenReturn(true);

    // When
    OverallStatistics statistics = BackupRunner.run(mockBackup);

    // Then
    verify(mockBackup).put("A");
    assertThat(statistics)
        .isEqualTo(
            new OverallStatistics(new Statistics(1, 0, 0, 0, 0, 0), new ErrorStatistics(0, 0, 0)));
  }

  @Test
  void whenDirectoryOnSourceAndNotDestination_skipsCreate() {
    // Given
    when(mockSource.scan()).thenReturn(Map.of("A", directory()));
    when(mockDestination.scan()).thenReturn(Map.of());

    // When
    OverallStatistics statistics = BackupRunner.run(mockBackup);

    // Then
    assertThat(statistics)
        .isEqualTo(
            new OverallStatistics(new Statistics(0, 0, 0, 0, 0, 0), new ErrorStatistics(0, 0, 0)));
  }

  @Test
  void whenFileOnSourceAndDestination_andNotSame_updatesFileOnDestination() {
    // Given
    Node.File sourceFile = file();
    when(mockSource.scan()).thenReturn(Map.of("A", sourceFile));
    Node.File destFile = file();
    when(mockDestination.scan()).thenReturn(Map.of("A", destFile));
    when(mockBackup.put(any())).thenReturn(true);
    when(sourceFile.same(destFile)).thenReturn(false);

    // When
    OverallStatistics statistics = BackupRunner.run(mockBackup);

    // Then
    verify(mockBackup).put("A");
    assertThat(statistics)
        .isEqualTo(
            new OverallStatistics(new Statistics(0, 1, 0, 0, 0, 0), new ErrorStatistics(0, 0, 0)));
  }

  @Test
  void whenFileOnSourceAndDestination_andSame_skipsUpdate() {
    // Given
    Node.File sourceFile = file();
    when(mockSource.scan()).thenReturn(Map.of("A", sourceFile));
    Node.File destFile = file();
    when(mockDestination.scan()).thenReturn(Map.of("A", destFile));
    when(sourceFile.same(destFile)).thenReturn(true);

    // When
    OverallStatistics statistics = BackupRunner.run(mockBackup);

    // Then
    assertThat(statistics)
        .isEqualTo(
            new OverallStatistics(new Statistics(0, 0, 0, 1, 0, 0), new ErrorStatistics(0, 0, 0)));
  }

  @Test
  void whenDirectoryOnSourceAndDestination_skipsUpdate() {
    // Given
    when(mockSource.scan()).thenReturn(Map.of("A", directory()));
    when(mockDestination.scan()).thenReturn(Map.of("A", directory()));

    // When
    OverallStatistics statistics = BackupRunner.run(mockBackup);

    // Then
    assertThat(statistics)
        .isEqualTo(
            new OverallStatistics(new Statistics(0, 0, 0, 0, 0, 0), new ErrorStatistics(0, 0, 0)));
  }

  @Test
  void whenFileOnSourceAndDirectoryOnDestination_overwritesDirectoryOnDestination() {
    // Given
    when(mockSource.scan()).thenReturn(Map.of("A", file()));
    when(mockDestination.scan()).thenReturn(Map.of("A", directory()));
    when(mockBackup.put(any())).thenReturn(true);

    // When
    OverallStatistics statistics = BackupRunner.run(mockBackup);

    // Then
    verify(mockBackup).put("A");
    assertThat(statistics)
        .isEqualTo(
            new OverallStatistics(new Statistics(1, 0, 0, 0, 0, 0), new ErrorStatistics(0, 0, 0)));
  }

  @Test
  void whenDirectoryOnSourceAndFileOnDestination_skipsUpdate() {
    // Given
    when(mockSource.scan()).thenReturn(Map.of("A", directory()));
    when(mockDestination.scan()).thenReturn(Map.of("A", file()));

    // When
    OverallStatistics statistics = BackupRunner.run(mockBackup);

    // Then
    assertThat(statistics)
        .isEqualTo(
            new OverallStatistics(new Statistics(0, 0, 0, 0, 0, 0), new ErrorStatistics(0, 0, 0)));
  }

  @Test
  void whenFileOnDestinationAndNotSource_deletesFileFromDestination() {
    // Given
    when(mockSource.scan()).thenReturn(Map.of());
    when(mockDestination.scan()).thenReturn(Map.of("A", file()));
    when(mockBackup.delete(any())).thenReturn(true);

    // When
    OverallStatistics statistics = BackupRunner.run(mockBackup);

    // Then
    verify(mockBackup).delete("A");
    assertThat(statistics)
        .isEqualTo(
            new OverallStatistics(new Statistics(0, 0, 1, 0, 0, 0), new ErrorStatistics(0, 0, 0)));
  }

  @Test
  void whenFileOnDestinationAndSource_skipsDelete() {
    // Given
    Node.File sourceFile = file();
    when(mockSource.scan()).thenReturn(Map.of("A", sourceFile));
    Node.File destFile = file();
    when(mockDestination.scan()).thenReturn(Map.of("A", destFile));
    when(sourceFile.same(destFile)).thenReturn(true);

    // When
    OverallStatistics statistics = BackupRunner.run(mockBackup);

    // Then
    assertThat(statistics)
        .isEqualTo(
            new OverallStatistics(new Statistics(0, 0, 0, 1, 0, 0), new ErrorStatistics(0, 0, 0)));
  }

  @Test
  void whenDirectoryOnDestinationAndNotSource_deletesDirectoryFromDestination() {
    // Given
    when(mockSource.scan()).thenReturn(Map.of());
    when(mockDestination.scan()).thenReturn(Map.of("A", directory()));
    when(mockBackup.delete(any())).thenReturn(true);

    // When
    OverallStatistics statistics = BackupRunner.run(mockBackup);

    // Then
    verify(mockBackup).delete("A");
    assertThat(statistics)
        .isEqualTo(
            new OverallStatistics(new Statistics(0, 0, 0, 0, 0, 0), new ErrorStatistics(0, 0, 0)));
  }

  @Test
  void whenDirectoryOnDestinationAndSource_skipsDelete() {
    // Given
    when(mockSource.scan()).thenReturn(Map.of("A", directory()));
    when(mockDestination.scan()).thenReturn(Map.of("A", directory()));

    // When
    OverallStatistics statistics = BackupRunner.run(mockBackup);

    // Then
    assertThat(statistics)
        .isEqualTo(
            new OverallStatistics(new Statistics(0, 0, 0, 0, 0, 0), new ErrorStatistics(0, 0, 0)));
  }

  @Test
  void whenCreateFails_countsFailedCreate() {
    // Given
    when(mockSource.scan()).thenReturn(Map.of("A", file()));
    when(mockDestination.scan()).thenReturn(Map.of());
    when(mockBackup.put(any())).thenReturn(false);

    // When
    OverallStatistics statistics = BackupRunner.run(mockBackup);

    // Then
    verify(mockBackup).put("A");
    assertThat(statistics)
        .isEqualTo(
            new OverallStatistics(new Statistics(0, 0, 0, 0, 0, 0), new ErrorStatistics(1, 0, 0)));
  }

  @Test
  void whenUpdateFails_countsFailedUpdate() {
    // Given
    when(mockSource.scan()).thenReturn(Map.of("A", file()));
    when(mockDestination.scan()).thenReturn(Map.of("A", file()));
    when(mockBackup.put(any())).thenReturn(false);

    // When
    OverallStatistics statistics = BackupRunner.run(mockBackup);

    // Then
    verify(mockBackup).put("A");
    assertThat(statistics)
        .isEqualTo(
            new OverallStatistics(new Statistics(0, 0, 0, 0, 0, 0), new ErrorStatistics(0, 1, 0)));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void whenDeleteFails_countsFailedDelete(boolean isDirectory) {
    // Given
    when(mockSource.scan()).thenReturn(Map.of());
    when(mockDestination.scan())
        .thenReturn(Map.of("A", isDirectory ? directory() : file()));
    when(mockBackup.delete(any())).thenReturn(false);

    // When
    OverallStatistics statistics = BackupRunner.run(mockBackup);

    // Then
    verify(mockBackup).delete("A");
    assertThat(statistics)
        .isEqualTo(
            new OverallStatistics(new Statistics(0, 0, 0, 0, 0, 0), new ErrorStatistics(0, 0, 1)));
  }

  private static Node.File file(){
    return mock(Node.File.class);
  }

  private static Node.Directory directory(){
    return mock(Node.Directory.class);
  }
}
