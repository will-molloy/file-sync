package com.willmolloy.backup;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

import java.nio.file.Path;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

/**
 * TrieBasedFileTreeTest.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
class TrieBasedFileTreeTest {

  @Test
  void preorder_returnsNodesInPreorder() {
    // Given
    TrieBasedFileTree<File> fileTree =
        TrieBasedFileTree.builder()
            .insert(file("A"))
            .insert(file("A/B"))
            .insert(file("A/B/C"))
            .insert(file("D"))
            .insert(file("D/E"))
            .insert(file("D/F"))
            .insert(file("X/Y/Z"))
            .build();

    // Then
    assertThat(fileTree.preorder())
        .containsExactly(
            file("A"),
            file("A/B"),
            file("A/B/C"),
            file("D"),
            file("D/E"),
            file("D/F"),
            file("X/Y/Z"))
        .inOrder();
  }

  @Test
  void preorder_withDirectoryFiller_returnsNodesIncludingMissingDirsInPreorder() {
    // Given
    TrieBasedFileTree<File> fileTree =
        TrieBasedFileTree.builder()
            .withDirectoryFiller(directoryFiller())
            .insert(file("A"))
            .insert(file("A/B"))
            .insert(file("A/B/C"))
            .insert(file("D"))
            .insert(file("D/E"))
            .insert(file("D/F"))
            .insert(file("X/Y/Z"))
            .build();

    // Then
    assertThat(fileTree.preorder())
        .containsExactly(
            directory(""),
            file("A"),
            file("A/B"),
            file("A/B/C"),
            file("D"),
            file("D/E"),
            file("D/F"),
            directory("X"),
            directory("X/Y"),
            file("X/Y/Z"))
        .inOrder();
  }

  @Test
  void leaves_returnsLeaves() {
    // Given
    TrieBasedFileTree<File> fileTree =
        TrieBasedFileTree.builder()
            .insert(file("A"))
            .insert(file("A/B"))
            .insert(file("A/B/C"))
            .insert(file("D"))
            .insert(file("D/E"))
            .insert(file("D/F"))
            .insert(file("X/Y/Z"))
            .build();

    // Then
    assertThat(fileTree.leaves())
        .containsExactly(file("A/B/C"), file("D/E"), file("D/F"), file("X/Y/Z"))
        .inOrder();
  }

  @Test
  void get_whenNodePresent_present() {
    File expected = file("A/B");
    TrieBasedFileTree<File> fileTree =
        TrieBasedFileTree.builder()
            .insert(file("A"))
            .insert(expected)
            .insert(file("A/B/C"))
            .build();
    assertThat(fileTree.get(expected.relativePath())).hasValue(expected);
  }

  @Test
  void get_whenNodeAbsent_empty() {
    File notExpected = file("A/B");
    TrieBasedFileTree<File> fileTree =
        TrieBasedFileTree.builder().insert(file("A")).insert(file("A/B/C")).build();
    assertThat(fileTree.get(notExpected.relativePath())).isEmpty();
  }

  @Test
  void get_withDirectoryFiller_whenMissingDirFilledIn_present() {
    File expected = file("A/B");
    TrieBasedFileTree<File> fileTree =
        TrieBasedFileTree.builder()
            .withDirectoryFiller(directoryFiller())
            .insert(file("A"))
            .insert(file("A/B/C"))
            .build();
    assertThat(fileTree.get(expected.relativePath())).hasValue(directory("A/B"));
  }

  @Test
  void get_withDirectoryFiller_whenMissingDirNotFilledIn_empty() {
    File notExpected = file("A/B/C/D");
    TrieBasedFileTree<File> fileTree =
        TrieBasedFileTree.builder()
            .withDirectoryFiller(directoryFiller())
            .insert(file("A"))
            .insert(file("A/B/C"))
            .build();
    assertThat(fileTree.get(notExpected.relativePath())).isEmpty();
  }

  @Test
  void ancestors_returnsAncestors() {
    TrieBasedFileTree<File> fileTree =
        TrieBasedFileTree.builder()
            .insert(directory("A"))
            .insert(directory("A/B/C"))
            .insert(file("A/B/C/D/E"))
            .insert(file("A/B/C/D/F"))
            .insert(file("A/B/C/D/X/Y/Z"))
            .build();
    assertThat(fileTree.ancestors(file("A/B/C/D/E")))
        .containsExactly(directory("A/B/C"), directory("A"))
        .inOrder();
  }

  @Test
  void ancestors_whenNodeNotInTree_empty() {
    TrieBasedFileTree<File> fileTree =
        TrieBasedFileTree.builder()
            .insert(directory("A"))
            .insert(directory("A/B/C"))
            .insert(file("A/B/C/D/E"))
            .insert(file("A/B/C/D/F"))
            .insert(file("A/B/C/D/X/Y/Z"))
            .build();
    assertThat(fileTree.ancestors(file("A/B/C/D"))).isEmpty();
  }

  @Test
  void ancestors_withDirectoryFiller_returnsAncestorsIncludingMissingDirs() {
    TrieBasedFileTree<File> fileTree =
        TrieBasedFileTree.builder()
            .withDirectoryFiller(directoryFiller())
            .insert(directory("A"))
            .insert(directory("A/B/C"))
            .insert(file("A/B/C/D/E"))
            .insert(file("A/B/C/D/F"))
            .insert(file("A/B/C/D/X/Y/Z"))
            .build();
    assertThat(fileTree.ancestors(file("A/B/C/D/E")))
        .containsExactly(directory("A/B/C/D"), directory("A/B/C"), directory("A/B"), directory("A"))
        .inOrder();
  }

  @Test
  void descendants_returnsDescendants() {
    TrieBasedFileTree<File> fileTree =
        TrieBasedFileTree.builder()
            .insert(directory("A"))
            .insert(directory("A/B/C"))
            .insert(file("A/B/C/D/E"))
            .insert(file("A/B/C/D/F"))
            .insert(file("A/B/C/D/X/Y/Z"))
            .build();
    assertThat(fileTree.descendants(file("A/B/C")))
        .containsExactly(file("A/B/C/D/E"), file("A/B/C/D/F"), file("A/B/C/D/X/Y/Z"))
        .inOrder();
  }

  @Test
  void descendants_whenNodeNotInTree_empty() {
    TrieBasedFileTree<File> fileTree =
        TrieBasedFileTree.builder()
            .insert(directory("A"))
            .insert(directory("A/B/C"))
            .insert(file("A/B/C/D/E"))
            .insert(file("A/B/C/D/F"))
            .insert(file("A/B/C/D/X/Y/Z"))
            .build();
    assertThat(fileTree.descendants(file("A/B/C/D"))).isEmpty();
  }

  @Test
  void descendants_withDirectoryFiller_returnsDescendantsIncludingMissingDirs() {
    TrieBasedFileTree<File> fileTree =
        TrieBasedFileTree.builder()
            .withDirectoryFiller(directoryFiller())
            .insert(directory("A"))
            .insert(directory("A/B/C"))
            .insert(file("A/B/C/D/E"))
            .insert(file("A/B/C/D/F"))
            .insert(file("A/B/C/D/X/Y/Z"))
            .build();
    assertThat(fileTree.descendants(file("A/B/C")))
        .containsExactly(
            directory("A/B/C/D"),
            file("A/B/C/D/E"),
            file("A/B/C/D/F"),
            directory("A/B/C/D/X"),
            directory("A/B/C/D/X/Y"),
            file("A/B/C/D/X/Y/Z"))
        .inOrder();
  }

  @Test
  void fileCount_countsFiles() {
    TrieBasedFileTree<File> fileTree =
        TrieBasedFileTree.builder()
            .insert(directory("A"))
            .insert(directory("A/B"))
            .insert(file("A/B/C"))
            .insert(file("A/B/D"))
            .insert(directory("X"))
            .insert(directory("X/Y"))
            .insert(file("X/Y/Z"))
            .build();
    assertThat(fileTree.fileCount()).isEqualTo(3);
  }

  @Test
  void totalSize_sumsFileSize() {
    TrieBasedFileTree<File> fileTree =
        TrieBasedFileTree.builder()
            .insert(directory("A"))
            .insert(directory("A/B"))
            .insert(file("A/B/C"))
            .insert(file("A/B/D"))
            .insert(directory("X"))
            .insert(directory("X/Y"))
            .insert(file("X/Y/Z"))
            .build();
    assertThat(fileTree.totalSize()).isEqualTo(6);
  }

  private static File file(String path) {
    return new TestFile(path, Path.of(path), 2, false);
  }

  private static File directory(String path) {
    return new TestFile(path, Path.of(path), 0, true);
  }

  private static Function<String, File> directoryFiller() {
    return path -> directory(path);
  }

  private record TestFile(String uri, Path relativePath, long size, boolean isDirectory)
      implements File {}
}
