package com.willmolloy.backup;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
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
  private BaseBackup<File, File> sut;

  @BeforeEach
  void setUp() {
    // kinda strange testing an abstract class like this... but I can't think of a better design atm
    sut =
        spy(
            new BaseBackup<>(mockSource, mockDest) {
              @Override
              protected boolean put(File sourceFile) {
                return true;
              }

              @Override
              protected boolean delete(File destFile) {
                return true;
              }
            });
  }

  @AfterEach
  void tearDown() {
    verify(sut).run();
    verifyNoMoreInteractions(sut);
  }

  @Test
  void whenFileOnlyOnSource_createsFileOnDestination() {
    // Given
    when(mockSource.fileTree()).thenReturn(fileTreeBuilder().insert(file("A")).build());
    when(mockDest.fileTree()).thenReturn(fileTreeBuilder().build());

    // When
    boolean result = sut.run();

    // Then
    assertThat(result).isTrue();
    verify(sut).needDelete(directory(""));
    verify(sut).needPut(file("A"));
    verify(sut).put(file("A"));
  }

  @Test
  void whenFileOnSourceAndDestination_andNotSame_updatesFileOnDestination() {
    // Given
    when(mockSource.fileTree()).thenReturn(fileTreeBuilder().insert(file("A")).build());
    when(mockDest.fileTree()).thenReturn(fileTreeBuilder().insert(differentFile("A")).build());

    // When
    boolean result = sut.run();

    // Then
    assertThat(result).isTrue();
    verify(sut).needDelete(differentFile("A"));
    verify(sut).needDelete(directory(""));
    verify(sut).needPut(file("A"));
    verify(sut).put(file("A"));
  }

  @Test
  void whenFileOnSourceAndDestination_andSame_skipsUpdate() {
    // Given
    when(mockSource.fileTree()).thenReturn(fileTreeBuilder().insert(file("A")).build());
    when(mockDest.fileTree()).thenReturn(fileTreeBuilder().insert(file("A")).build());

    // When
    boolean result = sut.run();

    // Then
    assertThat(result).isTrue();
    verify(sut).needDelete(file("A"));
    verify(sut).needDelete(directory(""));
    verify(sut).needPut(file("A"));
  }

  @Test
  void whenFileOnlyOnDestination_deletesFileFromDestination() {
    // Given
    when(mockSource.fileTree()).thenReturn(fileTreeBuilder().build());
    when(mockDest.fileTree()).thenReturn(fileTreeBuilder().insert(file("A")).build());

    // When
    boolean result = sut.run();

    // Then
    assertThat(result).isTrue();
    verify(sut).needDelete(file("A"));
    verify(sut, times(2)).needDelete(directory(""));
    verify(sut).delete(file("A"));
    verify(sut).needPut(directory(""));
  }

  @Test
  void whenFileOnDestinationAndSource_skipsDelete() {
    // Given
    when(mockSource.fileTree()).thenReturn(fileTreeBuilder().insert(file("A")).build());
    when(mockDest.fileTree()).thenReturn(fileTreeBuilder().insert(file("A")).build());

    // When
    boolean result = sut.run();

    // Then
    assertThat(result).isTrue();
    verify(sut).needDelete(file("A"));
    verify(sut).needDelete(directory(""));
    verify(sut).needPut(file("A"));
  }

  @Test
  void whenCreateFails_countsFailedCreate() {
    // Given
    when(mockSource.fileTree()).thenReturn(fileTreeBuilder().insert(file("A")).build());
    when(mockDest.fileTree()).thenReturn(fileTreeBuilder().build());
    when(sut.put(any())).thenReturn(false);

    // When
    boolean result = sut.run();

    // Then
    assertThat(result).isFalse();
    verify(sut).needDelete(directory(""));
    verify(sut).needPut(file("A"));
    verify(sut).put(file("A"));
  }

  @Test
  void whenUpdateFails_countsFailedUpdate() {
    // Given
    when(mockSource.fileTree()).thenReturn(fileTreeBuilder().insert(file("A")).build());
    when(mockDest.fileTree()).thenReturn(fileTreeBuilder().insert(differentFile("A")).build());
    when(sut.put(any())).thenReturn(false);

    // When
    boolean result = sut.run();

    // Then
    assertThat(result).isFalse();
    verify(sut).needDelete(differentFile("A"));
    verify(sut).needDelete(directory(""));
    verify(sut).needPut(file("A"));
    verify(sut).put(file("A"));
  }

  @Test
  void whenDeleteFails_countsFailedDelete() {
    // Given
    when(mockSource.fileTree()).thenReturn(fileTreeBuilder().build());
    when(mockDest.fileTree()).thenReturn(fileTreeBuilder().insert(file("A")).build());
    when(sut.delete(any())).thenReturn(false);

    // When
    boolean result = sut.run();

    // Then
    assertThat(result).isFalse();
    verify(sut).needDelete(file("A"));
    verify(sut, times(2)).needDelete(directory(""));
    verify(sut).delete(file("A"));
    verify(sut).needPut(directory(""));
  }

  @Test
  void whenChildExists_skipsPut() {
    // Given
    when(mockSource.fileTree())
        .thenReturn(fileTreeBuilder().insert(directory("A")).insert(file("A/B")).build());
    when(mockDest.fileTree()).thenReturn(fileTreeBuilder().build());

    // When
    boolean result = sut.run();

    // Then
    assertThat(result).isTrue();
    verify(sut).needDelete(directory(""));
    verify(sut).needPut(file("A/B"));
    verify(sut).put(file("A/B"));
  }

  @Test
  void whenGrandChildExists_skipsPut() {
    // Given
    when(mockSource.fileTree())
        .thenReturn(
            fileTreeBuilder()
                .insert(directory("A"))
                .insert(directory("A/B"))
                .insert(file("A/B/C"))
                .build());
    when(mockDest.fileTree()).thenReturn(fileTreeBuilder().build());

    // When
    boolean result = sut.run();

    // Then
    assertThat(result).isTrue();
    verify(sut).needDelete(directory(""));
    verify(sut).needPut(file("A/B/C"));
    verify(sut).put(file("A/B/C"));
  }

  @Test
  void whenParentExists_skipsDelete() {
    // Given
    when(mockSource.fileTree()).thenReturn(fileTreeBuilder().build());
    when(mockDest.fileTree())
        .thenReturn(fileTreeBuilder().insert(directory("A")).insert(file("A/B")).build());

    // When
    boolean result = sut.run();

    // Then
    assertThat(result).isTrue();
    verify(sut).needDelete(file("A/B"));
    verify(sut, times(2)).needDelete(directory("A"));
    verify(sut, times(2)).needDelete(directory(""));
    verify(sut).delete(directory("A"));
    verify(sut).needPut(directory(""));
  }

  @Test
  void whenGrandParentExists_skipsDelete() {
    // Given
    when(mockSource.fileTree()).thenReturn(fileTreeBuilder().build());
    when(mockDest.fileTree())
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
    verify(sut).needDelete(file("A/B/C"));
    verify(sut, times(2)).needDelete(directory("A/B"));
    verify(sut, times(2)).needDelete(directory("A"));
    verify(sut, times(2)).needDelete(directory(""));
    verify(sut).delete(directory("A"));
    verify(sut).needPut(directory(""));
  }

  @Test
  void whenParentExistsButParentInSource_deletesChild() {
    // Given
    when(mockSource.fileTree()).thenReturn(fileTreeBuilder().insert(directory("A")).build());
    when(mockDest.fileTree())
        .thenReturn(fileTreeBuilder().insert(directory("A")).insert(file("A/B")).build());

    // When
    boolean result = sut.run();

    // Then
    assertThat(result).isTrue();
    verify(sut).needDelete(file("A/B"));
    verify(sut, times(2)).needDelete(directory("A"));
    verify(sut, times(2)).needDelete(directory(""));
    verify(sut).delete(file("A/B"));
    verify(sut).needPut(directory("A"));
  }

  @Test
  void whenGrandParentExistsButGrandParentInSource_deletesGrandChild() {
    // Given
    when(mockSource.fileTree())
        .thenReturn(fileTreeBuilder().insert(directory("A")).insert(directory("A/B")).build());
    when(mockDest.fileTree())
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
    verify(sut).needDelete(file("A/B/C"));
    verify(sut, times(2)).needDelete(directory("A/B"));
    verify(sut, times(2)).needDelete(directory("A"));
    verify(sut, times(2)).needDelete(directory(""));
    verify(sut).delete(file("A/B/C"));
    verify(sut).needPut(directory("A/B"));
  }

  @Test
  void deletesBeforePut() {
    // Given
    when(mockSource.fileTree()).thenReturn(fileTreeBuilder().insert(file("A")).build());
    when(mockDest.fileTree()).thenReturn(fileTreeBuilder().insert(file("B")).build());

    // When
    boolean result = sut.run();

    // Then
    assertThat(result).isTrue();
    verify(sut).needDelete(file("B"));
    verify(sut, times(2)).needDelete(directory(""));
    verify(sut).needPut(file("A"));
    InOrder inOrder = inOrder(sut);
    inOrder.verify(sut).delete(file("B"));
    inOrder.verify(sut).put(file("A"));
  }

  private static FileTree.Builder<File> fileTreeBuilder() {
    return FileTree.builder(directory(""), BaseBackupTest::directory);
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

  private record TestFile(String uri, Path relativePath, long size, boolean isDirectory)
      implements File {}
}
