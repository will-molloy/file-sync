package com.willmolloy.backup;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * TrieBasedFileTreeTest.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
class TrieBasedFileTreeTest {

  @Test
  void get_whenNodeInTree_present() {
    File expected = file("A/B");
    TrieBasedFileTree<File> fileTree =
        new TrieBasedFileTree.Builder<>(directory(""), directoryFiller())
            .insert(file("A"))
            .insert(expected)
            .insert(file("A/B/C"))
            .build();
    assertThat(fileTree.get(expected.relativePath())).hasValue(expected);
  }

  @Test
  void get_whenMissingDirFilledIn_present() {
    File expected = file("A/B");
    TrieBasedFileTree<File> fileTree =
        new TrieBasedFileTree.Builder<>(directory(""), directoryFiller())
            .insert(file("A"))
            .insert(file("A/B/C"))
            .build();
    assertThat(fileTree.get(expected.relativePath())).hasValue(directory("A/B"));
  }

  @Test
  void get_whenNodeNotInTree_empty() {
    File notExpected = file("A/B/C/D");
    TrieBasedFileTree<File> fileTree =
        new TrieBasedFileTree.Builder<>(directory(""), directoryFiller())
            .insert(file("A"))
            .insert(file("A/B/C"))
            .build();
    assertThat(fileTree.get(notExpected.relativePath())).isEmpty();
  }

  @Test
  void postorder_returnsNodesIncludingMissingDirsInPostorder() {
    // Given
    TrieBasedFileTree<File> fileTree =
        new TrieBasedFileTree.Builder<>(directory(""), directoryFiller())
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
        new TrieBasedFileTree.Builder<>(directory(""), directoryFiller())
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
  void ancestors_returnsAncestorsIncludingMissingDirs() {
    // Given
    TrieBasedFileTree<File> fileTree =
        new TrieBasedFileTree.Builder<>(directory(""), directoryFiller())
            .insert(directory("A"))
            .insert(directory("A/B/C"))
            .insert(file("A/B/C/D/E"))
            .insert(file("A/B/C/D/F"))
            .insert(file("A/B/C/D/X/Y/Z"))
            .build();

    // Then
    assertThat(fileTree.ancestors(file("A/B/C/D")))
        .containsExactly(directory("A/B/C"), directory("A/B"), directory("A"), directory(""))
        .inOrder();
  }

  @Test
  void ancestors_whenNodeNotInTree_empty() {
    // Given
    TrieBasedFileTree<File> fileTree =
        new TrieBasedFileTree.Builder<>(directory(""), directoryFiller())
            .insert(directory("A"))
            .insert(directory("A/B/C"))
            .insert(file("A/B/C/D/E"))
            .insert(file("A/B/C/D/F"))
            .insert(file("A/B/C/D/X/Y/Z"))
            .build();

    // Then
    assertThat(fileTree.ancestors(file("X/Y/Z"))).isEmpty();
  }

  @Test
  void subtree_returnsSubtreeRootedAtGivenNodeIncludingMissingDirs() {
    // Given
    TrieBasedFileTree<File> fileTree =
        new TrieBasedFileTree.Builder<>(directory(""), directoryFiller())
            .insert(directory("A"))
            .insert(directory("A/B/C"))
            .insert(file("A/B/C/D/E"))
            .insert(file("A/B/C/D/F"))
            .insert(file("A/B/C/D/X/Y/Z"))
            .build();

    // When
    FileTree<File> subtree = fileTree.subtree(directory("A/B/C"));

    // Then
    assertThat(subtree)
        .isEqualTo(
            // null directoryFiller, otherwise it isn't tested!
            new TrieBasedFileTree.Builder<>(directory("A/B/C"), path -> null)
                .insert(directory("A/B/C/D"))
                .insert(file("A/B/C/D/E"))
                .insert(file("A/B/C/D/F"))
                .insert(directory("A/B/C/D/X"))
                .insert(directory("A/B/C/D/X/Y"))
                .insert(file("A/B/C/D/X/Y/Z"))
                .build());
    // verify ancestors ends at the new root
    assertThat(subtree.ancestors(file("A/B/C/D/X/Y/Z")))
        .containsExactly(
            directory("A/B/C/D/X/Y"),
            directory("A/B/C/D/X"),
            directory("A/B/C/D"),
            directory("A/B/C"))
        .inOrder();
  }

  @Test
  void subtree_whenFileNotInTree_returnsTreeContainingJustThatFile() {
    // Given
    TrieBasedFileTree<File> fileTree =
        new TrieBasedFileTree.Builder<>(directory(""), directoryFiller())
            .insert(directory("A"))
            .insert(directory("A/B/C"))
            .insert(file("A/B/C/D/E"))
            .insert(file("A/B/C/D/F"))
            .insert(file("A/B/C/D/X/Y/Z"))
            .build();
    File notInTree = file("X/Y/Z");

    // When
    FileTree<File> subtree = fileTree.subtree(notInTree);

    // Then
    assertThat(subtree)
        .isEqualTo(new TrieBasedFileTree.Builder<>(file("X/Y/Z"), path -> null).build());
  }

  @Test
  void fileCount_countsFiles() {
    TrieBasedFileTree<File> fileTree =
        new TrieBasedFileTree.Builder<>(directory(""), directoryFiller())
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
        new TrieBasedFileTree.Builder<>(directory(""), directoryFiller())
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

  private static DirectoryFiller<File> directoryFiller() {
    return TrieBasedFileTreeTest::directory;
  }

  private record TestFile(String uri, Path relativePath, long size, boolean isDirectory)
      implements File {}
}
