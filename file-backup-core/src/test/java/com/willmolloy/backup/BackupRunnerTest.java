package com.willmolloy.backup;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.Set;
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

  @Mock private Location<File> mockSource;
  @Mock private Location<File> mockDest;
  @Mock private Backup<File, File> mockBackup;

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
    when(mockSource.scan()).thenReturn(FileTree.fromSet(Set.of(file("A"))));
    when(mockDest.scan()).thenReturn(FileTree.empty());
    when(mockBackup.put(any())).thenReturn(true);

    // When
    boolean result = BackupRunner.run(mockBackup);

    // Then
    assertThat(result).isTrue();
    verify(mockBackup).put(file("A"));
  }

  @Test
  void whenFileOnSourceAndDestination_andNotSame_updatesFileOnDestination() {
    // Given
    when(mockSource.scan()).thenReturn(FileTree.fromSet(Set.of(file("A"))));
    when(mockDest.scan()).thenReturn(FileTree.fromSet(Set.of(differentFile("A"))));
    when(mockBackup.put(any())).thenReturn(true);

    // When
    boolean result = BackupRunner.run(mockBackup);

    // Then
    assertThat(result).isTrue();
    verify(mockBackup).put(file("A"));
  }

  @Test
  void whenFileOnSourceAndDestination_andSame_skipsUpdate() {
    // Given
    when(mockSource.scan()).thenReturn(FileTree.fromSet(Set.of(file("A"))));
    when(mockDest.scan()).thenReturn(FileTree.fromSet(Set.of(file("A"))));

    // When
    boolean result = BackupRunner.run(mockBackup);

    // Then
    assertThat(result).isTrue();
    verify(mockBackup, never()).put(file("A"));
  }

  @Test
  void whenFileOnlyOnDestination_deletesFileFromDestination() {
    // Given
    when(mockSource.scan()).thenReturn(FileTree.empty());
    when(mockDest.scan()).thenReturn(FileTree.fromSet(Set.of(file("A"))));
    when(mockBackup.delete(any())).thenReturn(true);

    // When
    boolean result = BackupRunner.run(mockBackup);

    // Then
    assertThat(result).isTrue();
    verify(mockBackup).delete(file("A"));
  }

  @Test
  void whenFileOnDestinationAndSource_skipsDelete() {
    // Given
    when(mockSource.scan()).thenReturn(FileTree.fromSet(Set.of(file("A"))));
    when(mockDest.scan()).thenReturn(FileTree.fromSet(Set.of(file("A"))));

    // When
    boolean result = BackupRunner.run(mockBackup);

    // Then
    assertThat(result).isTrue();
    verify(mockBackup, never()).delete(file("A"));
  }

  @Test
  void whenCreateFails_countsFailedCreate() {
    // Given
    when(mockSource.scan()).thenReturn(FileTree.fromSet(Set.of(file("A"))));
    when(mockDest.scan()).thenReturn(FileTree.empty());
    when(mockBackup.put(any())).thenReturn(false);

    // When
    boolean result = BackupRunner.run(mockBackup);

    // Then
    assertThat(result).isFalse();
    verify(mockBackup).put(file("A"));
  }

  @Test
  void whenUpdateFails_countsFailedUpdate() {
    // Given
    when(mockSource.scan()).thenReturn(FileTree.fromSet(Set.of(file("A"))));
    when(mockDest.scan()).thenReturn(FileTree.fromSet(Set.of(differentFile("A"))));
    when(mockBackup.put(any())).thenReturn(false);

    // When
    boolean result = BackupRunner.run(mockBackup);

    // Then
    assertThat(result).isFalse();
    verify(mockBackup).put(file("A"));
  }

  @Test
  void whenDeleteFails_countsFailedDelete() {
    // Given
    when(mockSource.scan()).thenReturn(FileTree.empty());
    when(mockDest.scan()).thenReturn(FileTree.fromSet(Set.of(file("A"))));
    when(mockBackup.delete(any())).thenReturn(false);

    // When
    boolean result = BackupRunner.run(mockBackup);

    // Then
    assertThat(result).isFalse();
    verify(mockBackup).delete(file("A"));
  }

  @Test
  void whenChildExists_skipsPut() {
    // Given
    when(mockSource.scan()).thenReturn(FileTree.fromSet(Set.of(file("A"), file("A/B"))));
    when(mockDest.scan()).thenReturn(FileTree.empty());
    when(mockBackup.put(any())).thenReturn(true);

    // When
    boolean result = BackupRunner.run(mockBackup);

    // Then
    assertThat(result).isTrue();
    verify(mockBackup, never()).put(file("A"));
    verify(mockBackup).put(file("A/B"));
  }

  @Test
  void whenGrandChildExists_skipsPut() {
    // Given
    when(mockSource.scan()).thenReturn(FileTree.fromSet(Set.of(file("A"), file("A/B/C"))));
    when(mockDest.scan()).thenReturn(FileTree.empty());
    when(mockBackup.put(any())).thenReturn(true);

    // When
    boolean result = BackupRunner.run(mockBackup);

    // Then
    assertThat(result).isTrue();
    verify(mockBackup, never()).put(file("A"));
    verify(mockBackup).put(file("A/B/C"));
  }

  @Test
  void whenParentExists_skipsDelete() {
    // Given
    when(mockSource.scan()).thenReturn(FileTree.empty());
    when(mockDest.scan()).thenReturn(FileTree.fromSet(Set.of(file("A"), file("A/B"))));
    when(mockBackup.delete(any())).thenReturn(true);

    // When
    boolean result = BackupRunner.run(mockBackup);

    // Then
    assertThat(result).isTrue();
    verify(mockBackup).delete(file("A"));
    verify(mockBackup, never()).delete(file("A/B"));
  }

  @Test
  void whenGrandParentExists_skipsDelete() {
    // Given
    when(mockSource.scan()).thenReturn(FileTree.empty());
    when(mockDest.scan()).thenReturn(FileTree.fromSet(Set.of(file("A"), file("A/B/C"))));
    when(mockBackup.delete(any())).thenReturn(true);

    // When
    boolean result = BackupRunner.run(mockBackup);

    // Then
    assertThat(result).isTrue();
    verify(mockBackup).delete(file("A"));
    verify(mockBackup, never()).delete(file("A/B/C"));
  }

  @Test
  void whenParentExistsButParentInSource_stillDeletes() {
    // Given
    when(mockSource.scan()).thenReturn(FileTree.fromSet(Set.of(file("A"))));
    when(mockDest.scan()).thenReturn(FileTree.fromSet(Set.of(file("A"), file("A/B"))));
    when(mockBackup.delete(any())).thenReturn(true);

    // When
    boolean result = BackupRunner.run(mockBackup);

    // Then
    assertThat(result).isTrue();
    verify(mockBackup, never()).delete(file("A"));
    verify(mockBackup).delete(file("A/B"));
  }

  @Test
  void whenGrandParentExistsButGrandParentInSource_stillDeletes() {
    // Given
    when(mockSource.scan()).thenReturn(FileTree.fromSet(Set.of(file("A"))));
    when(mockDest.scan()).thenReturn(FileTree.fromSet(Set.of(file("A"), file("A/B/C"))));
    when(mockBackup.delete(any())).thenReturn(true);

    // When
    boolean result = BackupRunner.run(mockBackup);

    // Then
    assertThat(result).isTrue();
    verify(mockBackup, never()).delete(file("A"));
    verify(mockBackup).delete(file("A/B/C"));
  }

  private static File file(String path) {
    return new TestFile(path, Path.of(path), 1, false);
  }

  private static File differentFile(String path) {
    return new TestFile(path, Path.of(path), 2, false);
  }

  private record TestFile(String uri, Path relativePath, long size, boolean isDirectory)
      implements File {}
}
