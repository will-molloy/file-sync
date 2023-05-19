package com.willmolloy.backup;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.willmolloy.backup.Backup.Location;
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
    when(mockSource.scan()).thenReturn(FileTree.from(Map.ofEntries(file("A"))));
    when(mockDest.scan()).thenReturn(emptyFileTree());
    when(mockBackup.put(any())).thenReturn(true);

    // When
    boolean result = BackupRunner.run(mockBackup);

    // Then
    assertThat(result).isTrue();
    verify(mockBackup).put(Path.of("A"));
  }

  @Test
  void whenFileOnSourceAndDestination_andNotSame_updatesFileOnDestination() {
    // Given
    var differentFile = file("A");
    when(differentFile.getValue().same(differentFile.getValue())).thenReturn(false);

    when(mockSource.scan()).thenReturn(FileTree.from(Map.ofEntries(differentFile)));
    when(mockDest.scan()).thenReturn(FileTree.from(Map.ofEntries(differentFile)));
    when(mockBackup.put(any())).thenReturn(true);

    // When
    boolean result = BackupRunner.run(mockBackup);

    // Then
    assertThat(result).isTrue();
    verify(mockBackup).put(Path.of("A"));
  }

  @Test
  void whenFileOnSourceAndDestination_andSame_skipsUpdate() {
    // Given
    var sameFile = file("A");
    when(sameFile.getValue().same(sameFile.getValue())).thenReturn(true);

    when(mockSource.scan()).thenReturn(FileTree.from(Map.ofEntries(sameFile)));
    when(mockDest.scan()).thenReturn(FileTree.from(Map.ofEntries(sameFile)));

    // When
    boolean result = BackupRunner.run(mockBackup);

    // Then
    assertThat(result).isTrue();
    verify(mockBackup, never()).put(Path.of("A"));
  }

  @Test
  void whenFileOnlyOnDestination_deletesFileFromDestination() {
    // Given
    when(mockSource.scan()).thenReturn(emptyFileTree());
    when(mockDest.scan()).thenReturn(FileTree.from(Map.ofEntries(file("A"))));
    when(mockBackup.delete(any())).thenReturn(true);

    // When
    boolean result = BackupRunner.run(mockBackup);

    // Then
    assertThat(result).isTrue();
    verify(mockBackup).delete(Path.of("A"));
  }

  @Test
  void whenFileOnDestinationAndSource_skipsDelete() {
    // Given
    var sameFile = file("A");
    when(sameFile.getValue().same(sameFile.getValue())).thenReturn(true);

    when(mockSource.scan()).thenReturn(FileTree.from(Map.ofEntries(sameFile)));
    when(mockDest.scan()).thenReturn(FileTree.from(Map.ofEntries(sameFile)));

    // When
    boolean result = BackupRunner.run(mockBackup);

    // Then
    assertThat(result).isTrue();
    verify(mockBackup, never()).delete(Path.of("A"));
  }

  @Test
  void whenCreateFails_countsFailedCreate() {
    // Given
    when(mockSource.scan()).thenReturn(FileTree.from(Map.ofEntries(file("A"))));
    when(mockDest.scan()).thenReturn(emptyFileTree());
    when(mockBackup.put(any())).thenReturn(false);

    // When
    boolean result = BackupRunner.run(mockBackup);

    // Then
    assertThat(result).isFalse();
    verify(mockBackup).put(Path.of("A"));
  }

  @Test
  void whenUpdateFails_countsFailedUpdate() {
    // Given
    when(mockSource.scan()).thenReturn(FileTree.from(Map.ofEntries(file("A"))));
    when(mockDest.scan()).thenReturn(FileTree.from(Map.ofEntries(file("A"))));
    when(mockBackup.put(any())).thenReturn(false);

    // When
    boolean result = BackupRunner.run(mockBackup);

    // Then
    assertThat(result).isFalse();
    verify(mockBackup).put(Path.of("A"));
  }

  @Test
  void whenDeleteFails_countsFailedDelete() {
    // Given
    when(mockSource.scan()).thenReturn(emptyFileTree());
    when(mockDest.scan()).thenReturn(FileTree.from(Map.ofEntries(file("A"))));
    when(mockBackup.delete(any())).thenReturn(false);

    // When
    boolean result = BackupRunner.run(mockBackup);

    // Then
    assertThat(result).isFalse();
    verify(mockBackup).delete(Path.of("A"));
  }

  @Test
  void whenPutCoveredByChild_skipsPut() {
    // Given
    when(mockSource.scan())
        .thenReturn(FileTree.from(Map.ofEntries(file("A"), file("A/B"), file("A/B/C"))));
    when(mockDest.scan()).thenReturn(emptyFileTree());
    when(mockBackup.put(any())).thenReturn(true);

    // When
    boolean result = BackupRunner.run(mockBackup);

    // Then
    assertThat(result).isTrue();
    verify(mockBackup, never()).put(Path.of("A"));
    verify(mockBackup, never()).put(Path.of("A/B"));
    verify(mockBackup).put(Path.of("A/B/C"));
  }

  @Test
  void whenDeleteCoveredByParent_skipsDelete() {
    // Given
    var root = file("A");
    when(root.getValue().same(root.getValue())).thenReturn(true);

    // cover 'parent NOT in source'
    when(mockSource.scan()).thenReturn(FileTree.from(Map.ofEntries(root)));
    when(mockDest.scan())
        .thenReturn(FileTree.from(Map.ofEntries(root, file("A/B"), file("A/B/C"))));
    when(mockBackup.delete(any())).thenReturn(true);

    // When
    boolean result = BackupRunner.run(mockBackup);

    // Then
    assertThat(result).isTrue();
    verify(mockBackup, never()).delete(Path.of("A"));
    verify(mockBackup).delete(Path.of("A/B"));
    verify(mockBackup, never()).delete(Path.of("A/B/C"));
  }

  private static Map.Entry<Path, File> file(String path) {
    return Map.entry(Path.of(path), mock(File.class));
  }

  private static FileTree emptyFileTree() {
    return FileTree.from(Map.of());
  }
}
