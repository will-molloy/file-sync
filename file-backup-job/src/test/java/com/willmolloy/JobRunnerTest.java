package com.willmolloy;

import static org.mockito.ArgumentMatchers.any;
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
 * JobRunnerTest. Ensures only the expected operations are run.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
@ExtendWith(MockitoExtension.class)
class JobRunnerTest {

  @Mock private Job mockJob;
  @InjectMocks private JobRunner jobRunner;

  @AfterEach
  void tearDown() {
    verify(mockJob).scanSource();
    verify(mockJob).scanDestination();
    verifyNoMoreInteractions(mockJob);
  }

  @Test
  void whenFilesOnlyOnSource_copiesFilesToDestination() {
    // Given
    when(mockJob.scanSource()).thenReturn(Stream.of(Path.of("A"), Path.of("B"), Path.of("C")));
    when(mockJob.scanDestination()).thenReturn(Stream.of());

    // When
    jobRunner.run();

    // Then
    verify(mockJob).copy(Path.of("A"));
    verify(mockJob).copy(Path.of("B"));
    verify(mockJob).copy(Path.of("C"));
  }

  @Test
  void whenFilesOnlyOnDestination_deletesFilesFromDestination() {
    // Given
    when(mockJob.scanSource()).thenReturn(Stream.of());
    when(mockJob.scanDestination()).thenReturn(Stream.of(Path.of("D"), Path.of("E"), Path.of("F")));

    // When
    jobRunner.run();

    // Then
    verify(mockJob).delete(Path.of("D"));
    verify(mockJob).delete(Path.of("E"));
    verify(mockJob).delete(Path.of("F"));
  }

  @Test
  void whenFilesOnSourceAndDestination_updatesFilesOnDestination() {
    // Given
    when(mockJob.scanSource()).thenReturn(Stream.of(Path.of("X"), Path.of("Y"), Path.of("Z")));
    when(mockJob.scanDestination()).thenReturn(Stream.of(Path.of("X"), Path.of("Y"), Path.of("Z")));

    when(mockJob.sourceNotEqualDestination(any())).thenReturn(true);

    // When
    jobRunner.run();

    // Then
    verify(mockJob).update(Path.of("X"));
    verify(mockJob).update(Path.of("Y"));
    verify(mockJob).update(Path.of("Z"));
  }

  @Test
  void copyDeleteAndUpdate() {
    // Given
    when(mockJob.scanSource())
        .thenReturn(
            Stream.of(
                Path.of("A"),
                Path.of("B"),
                Path.of("C"),
                Path.of("X"),
                Path.of("Y"),
                Path.of("Z")));
    when(mockJob.scanDestination())
        .thenReturn(
            Stream.of(
                Path.of("D"),
                Path.of("E"),
                Path.of("F"),
                Path.of("X"),
                Path.of("Y"),
                Path.of("Z")));

    when(mockJob.sourceNotEqualDestination(any())).thenReturn(true);

    // When
    jobRunner.run();

    // Then
    verify(mockJob).copy(Path.of("A"));
    verify(mockJob).copy(Path.of("B"));
    verify(mockJob).copy(Path.of("C"));
    verify(mockJob).delete(Path.of("D"));
    verify(mockJob).delete(Path.of("E"));
    verify(mockJob).delete(Path.of("F"));
    verify(mockJob).update(Path.of("X"));
    verify(mockJob).update(Path.of("Y"));
    verify(mockJob).update(Path.of("Z"));
  }

  @Test
  void whenDirectoryOnlyOnSource_copiesDirectoryToDestination() {
    // Given
    when(mockJob.scanSource())
        .thenReturn(Stream.of(Path.of("A"), Path.of("A/B"), Path.of("A/B/C")));
    when(mockJob.scanDestination()).thenReturn(Stream.of());

    // When
    jobRunner.run();

    // Then
    verify(mockJob).copy(Path.of("A/B/C"));
  }

  @Test
  void whenDirectoryOnlyOnDestination_deletesDirectoryFromDestination() {
    // Given
    when(mockJob.scanSource()).thenReturn(Stream.of());
    when(mockJob.scanDestination())
        .thenReturn(Stream.of(Path.of("A"), Path.of("A/B"), Path.of("A/B/C")));

    // When
    jobRunner.run();

    // Then
    verify(mockJob).delete(Path.of("A"));
  }

  @Test
  void whenDirectoryOnSourceAndDestination_updatesDirectoryOnDestination() {
    // Given
    when(mockJob.scanSource())
        .thenReturn(Stream.of(Path.of("A"), Path.of("A/B"), Path.of("A/B/C")));
    when(mockJob.scanDestination())
        .thenReturn(Stream.of(Path.of("A"), Path.of("A/B"), Path.of("A/B/C")));

    when(mockJob.sourceNotEqualDestination(any())).thenReturn(true);

    // When
    jobRunner.run();

    // Then
    verify(mockJob).update(Path.of("A/B/C"));
  }

  @Test
  void whenDirectoryOnSourceAndParentDirectoryOnDestination_copiesChildDirectoryToDestination() {
    // Given
    when(mockJob.scanSource())
        .thenReturn(Stream.of(Path.of("A"), Path.of("A/B"), Path.of("A/B/C")));
    when(mockJob.scanDestination()).thenReturn(Stream.of(Path.of("A"), Path.of("A/B")));

    // When
    jobRunner.run();

    // Then
    verify(mockJob).copy(Path.of("A/B/C"));
  }

  @Test
  void whenDirectoryOnSourceAndChildDirectoryOnDestination_deletesChildDirectoryFromDestination() {
    // Given
    when(mockJob.scanSource()).thenReturn(Stream.of(Path.of("A"), Path.of("A/B")));
    when(mockJob.scanDestination())
        .thenReturn(Stream.of(Path.of("A"), Path.of("A/B"), Path.of("A/B/C")));

    // When
    jobRunner.run();

    // Then
    verify(mockJob).delete(Path.of("A/B/C"));
  }
}
