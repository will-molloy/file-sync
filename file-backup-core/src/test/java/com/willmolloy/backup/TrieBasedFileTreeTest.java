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
  void postorder_returnsNodesInPostorder() {
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
    assertThat(fileTree.postorder())
        .containsExactly(
            file("A/B/C"),
            file("A/B"),
            file("A"),
            file("D/E"),
            file("D/F"),
            file("D"),
            file("X/Y/Z"))
        .inOrder();
  }

  @Test
  void postorder_withDirectoryFiller_returnsNodesIncludingMissingDirsInPostorder() {
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
    assertThat(fileTree.postorder())
        .containsExactly(
            file("A/B/C"),
            file("A/B"),
            file("A"),
            file("D/E"),
            file("D/F"),
            file("D"),
            file("X/Y/Z"),
            directory("X/Y"),
            directory("X"),
            directory(""))
        .inOrder();
  }

  @Test
  void leaves_returnsLeavesLeftToRight() {
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
    assertThat(fileTree.ancestors(file("A/B/C/D")))
        .containsExactly(directory("A/B/C"), directory("A/B"), directory("A"), directory(""))
        .inOrder();
  }

  @Test
  void subtree_returnsSubtreeRootedAtGivenNode() {
    // Given
    TrieBasedFileTree<File> fileTree =
        TrieBasedFileTree.builder()
            .insert(directory("A"))
            .insert(directory("A/B/C"))
            .insert(file("A/B/C/D/E"))
            .insert(file("A/B/C/D/F"))
            .insert(file("A/B/C/D/X/Y/Z"))
            .build();

    // When
    FileTree<File> subtree = fileTree.subtree(directory("A/B/C"));

    // Then
    // building the subtree complicates the implementation, so testing via post-order/ancestors
    assertThat(subtree.postorder())
        .containsExactly(
            file("A/B/C/D/E"), file("A/B/C/D/F"), file("A/B/C/D/X/Y/Z"), directory("A/B/C"))
        .inOrder();
    assertThat(subtree.ancestors(file("A/B/C/D/X/Y/Z"))).containsExactly(directory("A/B/C"));
  }

  @Test
  void subtree_whenNodeNotInTree_empty() {
    TrieBasedFileTree<File> fileTree =
        TrieBasedFileTree.builder()
            .insert(directory("A"))
            .insert(directory("A/B/C"))
            .insert(file("A/B/C/D/E"))
            .insert(file("A/B/C/D/F"))
            .insert(file("A/B/C/D/X/Y/Z"))
            .build();
    assertThat(fileTree.subtree(directory("A/B/C/D")))
        .isEqualTo(TrieBasedFileTree.builder().build());
  }

  @Test
  void subtree_withDirectoryFiller_returnsSubtreeRootedAtGivenNodeIncludingMissingDirs() {
    // Given
    TrieBasedFileTree<File> fileTree =
        TrieBasedFileTree.builder()
            .withDirectoryFiller(directoryFiller())
            .insert(directory("A"))
            .insert(directory("A/B/C"))
            .insert(file("A/B/C/D/E"))
            .insert(file("A/B/C/D/F"))
            .insert(file("A/B/C/D/X/Y/Z"))
            .build();

    // When
    FileTree<File> subtree = fileTree.subtree(directory("A/B/C/D"));

    // Then
    // building the subtree complicates the implementation, so testing via post-order
    assertThat(subtree.postorder())
        .containsExactly(
            file("A/B/C/D/E"),
            file("A/B/C/D/F"),
            file("A/B/C/D/X/Y/Z"),
            directory("A/B/C/D/X/Y"),
            directory("A/B/C/D/X"),
            directory("A/B/C/D"))
        .inOrder();
    assertThat(subtree.ancestors(file("A/B/C/D/X/Y/Z")))
        .containsExactly(directory("A/B/C/D/X/Y"), directory("A/B/C/D/X"), directory("A/B/C/D"));
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
