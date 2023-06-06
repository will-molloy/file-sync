package com.willmolloy.backup;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.willmolloy.backup.statistics.DiscordWebhook;
import com.willmolloy.backup.statistics.LoggingBackupObserver;
import com.willmolloy.backup.statistics.Statistics;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link BaseBackup} unit test.
 *
 * <p>Verifies the exact minimal operations occur.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
@SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
@ExtendWith(MockitoExtension.class)
class BaseBackupTest {

  @Mock private Location<File> mockSource;
  @Mock private Location<File> mockDest;
  @Spy private LoggingBackupObserver observer;
  private BaseBackup<File, File> sut;

  @BeforeEach
  void setUp() {
    // kinda strange testing an abstract class like this... but I can't think of a better design atm
    sut =
        spy(
            new BaseBackup<>(mockSource, mockDest, List.of(observer)) {
              @Override
              protected boolean put(File sourceFile) {
                return true;
              }

              @Override
              protected boolean delete(FileTree<File> destFile) {
                return true;
              }
            });
  }

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(mockSource);
    verifyNoMoreInteractions(mockDest);
    verify(observer).notifyStarted(same(sut));
    verify(observer).notifyScanned(same(mockSource), any(), any());
    verify(observer).notifyScanned(same(mockDest), any(), any());
    verifyNoMoreInteractions(observer);
    verify(sut).run();
    verifyNoMoreInteractions(sut);
  }

  @Test
  void whenFileOnlyOnSource_createsFileOnDestination() {
    // Given
    when(mockSource.scan()).thenReturn(fileTreeBuilder().insert(file("A")).build());
    when(mockDest.scan()).thenReturn(fileTreeBuilder().build());

    // When
    boolean result = sut.run();

    // Then
    assertThat(result).isTrue();
    verify(observer)
        .notifyFinished(same(sut), eq(new Statistics.Snapshot(1, 0, 0, 0, 0, 0, 0, 1, 0)), any());
    verify(sut).needCreate(file("A"), Optional.empty());
    verify(sut).put(file("A"));
  }

  @Test
  void whenFileOnSourceAndDestination_andNotSame_updatesFileOnDestination() {
    // Given
    when(mockSource.scan()).thenReturn(fileTreeBuilder().insert(file("A")).build());
    when(mockDest.scan()).thenReturn(fileTreeBuilder().insert(differentFile("A")).build());

    // When
    boolean result = sut.run();

    // Then
    assertThat(result).isTrue();
    verify(observer)
        .notifyFinished(same(sut), eq(new Statistics.Snapshot(0, 1, 0, 0, 0, 0, 0, 1, 2)), any());
    verify(sut, times(2)).needDelete(Optional.of(file("A")), differentFile("A"));
    verify(sut).needCreate(file("A"), Optional.of(differentFile("A")));
    verify(sut).needUpdate(file("A"), differentFile("A"));
    verify(sut).put(file("A"));
  }

  @Test
  void whenFileOnSourceAndDestination_andSame_skipsUpdate() {
    // Given
    when(mockSource.scan()).thenReturn(fileTreeBuilder().insert(file("A")).build());
    when(mockDest.scan()).thenReturn(fileTreeBuilder().insert(file("A")).build());

    // When
    boolean result = sut.run();

    // Then
    assertThat(result).isTrue();
    verify(observer)
        .notifyFinished(same(sut), eq(new Statistics.Snapshot(0, 0, 0, 1, 0, 0, 0, 0, 0)), any());
    verify(sut, times(2)).needDelete(Optional.of(file("A")), file("A"));
    verify(sut).needCreate(file("A"), Optional.of(file("A")));
    verify(sut).needUpdate(file("A"), file("A"));
  }

  @Test
  void whenFileOnlyOnDestination_deletesFileFromDestination() {
    // Given
    when(mockSource.scan()).thenReturn(fileTreeBuilder().build());
    when(mockDest.scan()).thenReturn(fileTreeBuilder().insert(file("A")).build());

    // When
    boolean result = sut.run();

    // Then
    assertThat(result).isTrue();
    verify(observer)
        .notifyFinished(same(sut), eq(new Statistics.Snapshot(0, 0, 1, 0, 0, 0, 0, 0, 1)), any());
    verify(sut).needDelete(Optional.empty(), file("A"));
    verify(sut).delete(argThat(rootedAt(file("A"))));
  }

  @Test
  void whenFileOnDestinationAndSource_skipsDelete() {
    // Given
    when(mockSource.scan()).thenReturn(fileTreeBuilder().insert(file("A")).build());
    when(mockDest.scan()).thenReturn(fileTreeBuilder().insert(file("A")).build());

    // When
    boolean result = sut.run();

    // Then
    assertThat(result).isTrue();
    verify(observer)
        .notifyFinished(same(sut), eq(new Statistics.Snapshot(0, 0, 0, 1, 0, 0, 0, 0, 0)), any());
    verify(sut, times(2)).needDelete(Optional.of(file("A")), file("A"));
    verify(sut).needCreate(file("A"), Optional.of(file("A")));
    verify(sut).needUpdate(file("A"), file("A"));
  }

  @Test
  void whenCreateFails_countsFailedCreate() {
    // Given
    when(mockSource.scan()).thenReturn(fileTreeBuilder().insert(file("A")).build());
    when(mockDest.scan()).thenReturn(fileTreeBuilder().build());
    when(sut.put(any())).thenReturn(false);

    // When
    boolean result = sut.run();

    // Then
    assertThat(result).isFalse();
    verify(observer)
        .notifyFinished(same(sut), eq(new Statistics.Snapshot(0, 0, 0, 0, 1, 0, 0, 0, 0)), any());
    verify(sut).needCreate(file("A"), Optional.empty());
    verify(sut).put(file("A"));
  }

  @Test
  void whenUpdateFails_countsFailedUpdate() {
    // Given
    when(mockSource.scan()).thenReturn(fileTreeBuilder().insert(file("A")).build());
    when(mockDest.scan()).thenReturn(fileTreeBuilder().insert(differentFile("A")).build());
    when(sut.put(any())).thenReturn(false);

    // When
    boolean result = sut.run();

    // Then
    assertThat(result).isFalse();
    verify(observer)
        .notifyFinished(same(sut), eq(new Statistics.Snapshot(0, 0, 0, 0, 0, 1, 0, 0, 0)), any());
    verify(sut, times(2)).needDelete(Optional.of(file("A")), differentFile("A"));
    verify(sut).needCreate(file("A"), Optional.of(differentFile("A")));
    verify(sut).needUpdate(file("A"), differentFile("A"));
    verify(sut).put(file("A"));
  }

  @Test
  void whenDeleteFails_countsFailedDelete() {
    // Given
    when(mockSource.scan()).thenReturn(fileTreeBuilder().build());
    when(mockDest.scan()).thenReturn(fileTreeBuilder().insert(file("A")).build());
    when(sut.delete(any())).thenReturn(false);

    // When
    boolean result = sut.run();

    // Then
    assertThat(result).isFalse();
    verify(observer)
        .notifyFinished(same(sut), eq(new Statistics.Snapshot(0, 0, 0, 0, 0, 0, 1, 0, 0)), any());
    verify(sut).needDelete(Optional.empty(), file("A"));
    verify(sut).delete(argThat(rootedAt(file("A"))));
  }

  @Test
  void whenChildExists_onlyPutsChild() {
    // Given
    when(mockSource.scan())
        .thenReturn(fileTreeBuilder().insert(directory("A")).insert(file("A/B")).build());
    when(mockDest.scan()).thenReturn(fileTreeBuilder().build());

    // When
    boolean result = sut.run();

    // Then
    assertThat(result).isTrue();
    verify(observer)
        .notifyFinished(same(sut), eq(new Statistics.Snapshot(1, 0, 0, 0, 0, 0, 0, 1, 0)), any());
    verify(sut).needCreate(file("A/B"), Optional.empty());
    verify(sut).put(file("A/B"));
  }

  @Test
  void whenGrandChildExists_onlyPutsGrandChild() {
    // Given
    when(mockSource.scan())
        .thenReturn(
            fileTreeBuilder()
                .insert(directory("A"))
                .insert(directory("A/B"))
                .insert(file("A/B/C"))
                .build());
    when(mockDest.scan()).thenReturn(fileTreeBuilder().build());

    // When
    boolean result = sut.run();

    // Then
    assertThat(result).isTrue();
    verify(observer)
        .notifyFinished(same(sut), eq(new Statistics.Snapshot(1, 0, 0, 0, 0, 0, 0, 1, 0)), any());
    verify(sut).needCreate(file("A/B/C"), Optional.empty());
    verify(sut).put(file("A/B/C"));
  }

  @Test
  void whenParentExists_onlyDeletesParent() {
    // Given
    when(mockSource.scan()).thenReturn(fileTreeBuilder().build());
    when(mockDest.scan())
        .thenReturn(fileTreeBuilder().insert(directory("A")).insert(file("A/B")).build());

    // When
    boolean result = sut.run();

    // Then
    assertThat(result).isTrue();
    verify(observer)
        .notifyFinished(same(sut), eq(new Statistics.Snapshot(0, 0, 1, 0, 0, 0, 0, 0, 1)), any());
    verify(sut).needDelete(Optional.empty(), file("A/B"));
    verify(sut, times(2)).needDelete(Optional.empty(), directory("A"));
    verify(sut).delete(argThat(rootedAt(directory("A"))));
  }

  @Test
  void whenGrandParentExists_onlyDeletesGrandParent() {
    // Given
    when(mockSource.scan()).thenReturn(fileTreeBuilder().build());
    when(mockDest.scan())
        .thenReturn(
            fileTreeBuilder()
                .insert(directory("A"))
                .insert(directory("A/B"))
                .insert(file("A/B/C"))
                .build());

    // When
    boolean result = sut.run();

    // Then
    assertThat(result).isTrue();
    verify(observer)
        .notifyFinished(same(sut), eq(new Statistics.Snapshot(0, 0, 1, 0, 0, 0, 0, 0, 1)), any());
    verify(sut).needDelete(Optional.empty(), file("A/B/C"));
    verify(sut, times(2)).needDelete(Optional.empty(), directory("A/B"));
    verify(sut, times(2)).needDelete(Optional.empty(), directory("A"));
    verify(sut).delete(argThat(rootedAt(directory("A"))));
  }

  @Test
  void whenParentExistsButParentInSource_stillDeletesChild() {
    // Given
    when(mockSource.scan()).thenReturn(fileTreeBuilder().insert(directory("A")).build());
    when(mockDest.scan())
        .thenReturn(fileTreeBuilder().insert(directory("A")).insert(file("A/B")).build());

    // When
    boolean result = sut.run();

    // Then
    assertThat(result).isTrue();
    verify(observer)
        .notifyFinished(same(sut), eq(new Statistics.Snapshot(0, 0, 1, 1, 0, 0, 0, 0, 1)), any());
    verify(sut).needDelete(Optional.empty(), file("A/B"));
    verify(sut, times(3)).needDelete(Optional.of(directory("A")), directory("A"));
    verify(sut).delete(argThat(rootedAt(file("A/B"))));
    verify(sut).needCreate(directory("A"), Optional.of(directory("A")));
    verify(sut).needUpdate(directory("A"), directory("A"));
  }

  @Test
  void whenGrandParentExistsButGrandParentInSource_stillDeletesGrandChild() {
    // Given
    when(mockSource.scan())
        .thenReturn(fileTreeBuilder().insert(directory("A")).insert(directory("A/B")).build());
    when(mockDest.scan())
        .thenReturn(
            fileTreeBuilder()
                .insert(directory("A"))
                .insert(directory("A/B"))
                .insert(file("A/B/C"))
                .build());

    // When
    boolean result = sut.run();

    // Then
    assertThat(result).isTrue();
    verify(observer)
        .notifyFinished(same(sut), eq(new Statistics.Snapshot(0, 0, 1, 1, 0, 0, 0, 0, 1)), any());
    verify(sut).needDelete(Optional.empty(), file("A/B/C"));
    verify(sut, times(3)).needDelete(Optional.of(directory("A/B")), directory("A/B"));
    verify(sut, times(2)).needDelete(Optional.of(directory("A")), directory("A"));
    verify(sut).delete(argThat(rootedAt(file("A/B/C"))));
    verify(sut).needCreate(directory("A/B"), Optional.of(directory("A/B")));
    verify(sut).needUpdate(directory("A/B"), directory("A/B"));
  }

  @Test
  void deletesBeforePut() {
    // Given
    when(mockSource.scan()).thenReturn(fileTreeBuilder().insert(file("A")).build());
    when(mockDest.scan()).thenReturn(fileTreeBuilder().insert(file("B")).build());

    // When
    boolean result = sut.run();

    // Then
    assertThat(result).isTrue();
    verify(observer)
        .notifyFinished(same(sut), eq(new Statistics.Snapshot(1, 0, 1, 0, 0, 0, 0, 1, 1)), any());
    verify(sut).needDelete(Optional.empty(), file("B"));
    verify(sut).needCreate(file("A"), Optional.empty());
    InOrder inOrder = inOrder(sut);
    inOrder.verify(sut).delete(argThat(rootedAt(file("B"))));
    inOrder.verify(sut).put(file("A"));
  }

  @Test
  void whenDeleteBeforeUpdate_countsAsCreate() {
    // Given
    when(mockSource.scan()).thenReturn(fileTreeBuilder().insert(file("B")).build());
    when(mockDest.scan()).thenReturn(fileTreeBuilder().insert(differentFile("B")).build());
    when(sut.needDelete(Optional.of(file("B")), differentFile("B"))).thenReturn(true);

    // When
    boolean result = sut.run();

    // Then
    assertThat(result).isTrue();
    verify(observer)
        .notifyFinished(same(sut), eq(new Statistics.Snapshot(1, 0, 1, 0, 0, 0, 0, 1, 2)), any());
    verify(sut, times(2)).needDelete(Optional.of(file("B")), differentFile("B"));
    verify(sut).needCreate(file("B"), Optional.of(differentFile("B")));
    InOrder inOrder = inOrder(sut);
    inOrder.verify(sut).delete(argThat(rootedAt(differentFile("B"))));
    inOrder.verify(sut).put(file("B"));
  }

  private static FileTree.Builder<File> fileTreeBuilder() {
    return FileTree.builder(directory(""));
  }

  private static File directory(String path) {
    return new TestFile(path, Path.of(path), 0, true);
  }

  private static File file(String path) {
    return new TestFile(path, Path.of(path), 1, false);
  }

  private static File differentFile(String path) {
    return new TestFile(path, Path.of(path), 2, false);
  }

  private static ArgumentMatcher<FileTree<File>> rootedAt(File file) {
    return actual -> actual.root().equals(file);
  }

  private record TestFile(String uri, Path relativePath, long size, boolean isDirectory)
      implements File {}
}
