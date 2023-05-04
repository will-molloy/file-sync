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
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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

  @ParameterizedTest
  @MethodSource(value = "filesNotEqual")
  void whenFileOnSourceAndDestination_andNotEqual_updatesFileOnDestination(
      TestFile sourceFile, TestFile destFile) {
    // Given
    when(mockSource.scan()).thenReturn(Map.of("A", sourceFile));
    when(mockDestination.scan()).thenReturn(Map.of("A", destFile));
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
                new TestFile(OptionalLong.of(1), Optional.empty()),
                "E",
                new TestFile(OptionalLong.empty(), Optional.of(Instant.ofEpochSecond(1))),
                "F",
                new TestFile(OptionalLong.of(1), Optional.of(Instant.ofEpochSecond(1)))));
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

  @ParameterizedTest
  @MethodSource(value = "filesNotEqual")
  void whenUpdateFails_countsFailedUpdate(TestFile sourceFile, TestFile destFile) {
    // Given
    when(mockSource.scan()).thenReturn(Map.of("A", sourceFile));
    when(mockDestination.scan()).thenReturn(Map.of("A", destFile));
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

  static Stream<Arguments> filesNotEqual() {
    return Stream.of(
        Arguments.of(
            new TestFile(OptionalLong.of(2), Optional.of(Instant.ofEpochSecond(1))),
            new TestFile(OptionalLong.of(1), Optional.of(Instant.ofEpochSecond(1)))),
        Arguments.of(
            new TestFile(OptionalLong.of(2), Optional.of(Instant.ofEpochSecond(2))),
            new TestFile(OptionalLong.of(2), Optional.of(Instant.ofEpochSecond(1)))),
        Arguments.of(
            new TestFile(OptionalLong.of(1), Optional.empty()),
            new TestFile(OptionalLong.empty(), Optional.empty())),
        Arguments.of(
            new TestFile(OptionalLong.empty(), Optional.of(Instant.ofEpochSecond(1))),
            new TestFile(OptionalLong.empty(), Optional.empty())));
  }

  private record TestFile(OptionalLong size, Optional<Instant> lastModified) implements File {
    private TestFile() {
      this(OptionalLong.empty(), Optional.empty());
    }
  }
}
