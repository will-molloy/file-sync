package com.willmolloy;

import static org.junit.jupiter.api.Assertions.*;
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
 * JobRunnerTest.
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
    verify(mockJob).copyToDestination(Path.of("A"));
    verify(mockJob).copyToDestination(Path.of("B"));
    verify(mockJob).copyToDestination(Path.of("C"));
  }

  @Test
  void whenFilesOnlyOnDestination_deletesFilesFromDestination() {
    // Given
    when(mockJob.scanSource()).thenReturn(Stream.of());
    when(mockJob.scanDestination()).thenReturn(Stream.of(Path.of("D"), Path.of("E"), Path.of("F")));

    // When
    jobRunner.run();

    // Then
    verify(mockJob).deleteFromDestination(Path.of("D"));
    verify(mockJob).deleteFromDestination(Path.of("E"));
    verify(mockJob).deleteFromDestination(Path.of("F"));
  }

  @Test
  void whenFilesOnSourceAndDestination_updatesFilesOnDestination() {
    // Given
    when(mockJob.scanSource()).thenReturn(Stream.of(Path.of("X"), Path.of("Y"), Path.of("Z")));
    when(mockJob.scanDestination()).thenReturn(Stream.of(Path.of("X"), Path.of("Y"), Path.of("X")));

    when(mockJob.sourceNotEqualDestination(any())).thenReturn(true);

    // When
    jobRunner.run();

    // Then
    verify(mockJob).copyToDestination(Path.of("X"));
    verify(mockJob).copyToDestination(Path.of("Y"));
    verify(mockJob).copyToDestination(Path.of("Z"));
  }

  @Test
  void allThreeConditionsAtOnce() {
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
                Path.of("X")));

    when(mockJob.sourceNotEqualDestination(any())).thenReturn(true);

    // When
    jobRunner.run();

    // Then
    verify(mockJob).copyToDestination(Path.of("A"));
    verify(mockJob).copyToDestination(Path.of("B"));
    verify(mockJob).copyToDestination(Path.of("C"));
    verify(mockJob).deleteFromDestination(Path.of("D"));
    verify(mockJob).deleteFromDestination(Path.of("E"));
    verify(mockJob).deleteFromDestination(Path.of("F"));
    verify(mockJob).copyToDestination(Path.of("X"));
    verify(mockJob).copyToDestination(Path.of("Y"));
    verify(mockJob).copyToDestination(Path.of("Z"));
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
    verify(mockJob).copyToDestination(Path.of("A/B/C"));
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
    verify(mockJob).deleteFromDestination(Path.of("A"));
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
    verify(mockJob).copyToDestination(Path.of("A/B/C"));
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
    verify(mockJob).copyToDestination(Path.of("A/B/C"));
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
    verify(mockJob).deleteFromDestination(Path.of("A/B/C"));
  }
}
