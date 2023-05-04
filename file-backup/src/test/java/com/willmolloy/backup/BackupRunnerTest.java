package com.willmolloy.backup;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.willmolloy.backup.Backup.File;
import com.willmolloy.backup.Backup.Location;
import com.willmolloy.backup.BackupRunner.Statistics;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
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

    // When
    Statistics statistics = backupRunner.run();

    // Then
    verify(mockBackup).put("A");
    assertThat(statistics).isEqualTo(new Statistics(1, 0, 0));
  }

  @Test
  void whenFileOnSourceAndDestination_andDifferentSize_updatesFileOnDestination() {
    // Given
    when(mockSource.scan())
        .thenReturn(
            Map.of("A", new TestFile(OptionalLong.of(2), Optional.of(Instant.ofEpochSecond(1)))));
    when(mockDestination.scan())
        .thenReturn(
            Map.of("A", new TestFile(OptionalLong.of(1), Optional.of(Instant.ofEpochSecond(1)))));

    // When
    Statistics statistics = backupRunner.run();

    // Then
    verify(mockBackup).put("A");
    assertThat(statistics).isEqualTo(new Statistics(0, 1, 0));
  }

  @Test
  void whenFileOnSourceAndDestination_andDifferentModifiedTime_updatesFileOnDestination() {
    // Given
    when(mockSource.scan())
        .thenReturn(
            Map.of("A", new TestFile(OptionalLong.of(2), Optional.of(Instant.ofEpochSecond(2)))));
    when(mockDestination.scan())
        .thenReturn(
            Map.of("A", new TestFile(OptionalLong.of(2), Optional.of(Instant.ofEpochSecond(1)))));

    // When
    Statistics statistics = backupRunner.run();

    // Then
    verify(mockBackup).put("A");
    assertThat(statistics).isEqualTo(new Statistics(0, 1, 0));
  }

  @Test
  void whenFileOnSourceAndDestination_andEqual_skipsUpdate() {
    // Given
    when(mockSource.scan())
        .thenReturn(
            Map.of("A", new TestFile(OptionalLong.of(2), Optional.of(Instant.ofEpochSecond(2)))));
    when(mockDestination.scan())
        .thenReturn(
            Map.of("A", new TestFile(OptionalLong.of(2), Optional.of(Instant.ofEpochSecond(2)))));

    // When
    Statistics statistics = backupRunner.run();

    // Then
    verify(mockBackup, never()).put("A");
    assertThat(statistics).isEqualTo(new Statistics(0, 0, 0));
  }

  @Test
  void whenFileOnDestinationAndNotSource_deletesFileFromDestination() {
    // Given
    when(mockSource.scan()).thenReturn(Map.of());
    when(mockDestination.scan()).thenReturn(Map.of("A", new TestFile()));

    // When
    Statistics statistics = backupRunner.run();

    // Then
    verify(mockBackup).delete("A");
    assertThat(statistics).isEqualTo(new Statistics(0, 0, 1));
  }

  @Test
  void whenFileOnDestinationAndSource_skipsDelete() {
    // Given
    when(mockSource.scan()).thenReturn(Map.of("A", new TestFile()));
    when(mockDestination.scan()).thenReturn(Map.of("A", new TestFile()));

    // When
    Statistics statistics = backupRunner.run();

    // Then
    verify(mockBackup, never()).delete("A");
    assertThat(statistics).isEqualTo(new Statistics(0, 0, 0));
  }

  private record TestFile(OptionalLong size, Optional<Instant> lastModified) implements File {
    private TestFile() {
      this(OptionalLong.empty(), Optional.empty());
    }
  }
}
