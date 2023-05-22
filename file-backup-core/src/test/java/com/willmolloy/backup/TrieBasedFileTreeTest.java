package com.willmolloy.backup;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * TrieBasedFileTreeTest.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
class TrieBasedFileTreeTest {

  @Test
  void forEach_visitsEachNodeExactlyOnce() {
    // Given
    Set<File> expected =
        Set.of(
            file("A"),
            file("A/B"),
            file("A/B/C"),
            file("D"),
            file("D/E"),
            file("D/F"),
            file("X/Y/Z"));
    FileTree<File> fileTree = TrieBasedFileTree.from(expected);

    // When
    List<File> actual = new ArrayList<>();
    fileTree.forEach(actual::add);

    // Then
    assertThat(actual).containsExactlyElementsIn(expected);
  }

  @Test
  void get_presentWhenNodePresent() {
    File expected = file("A/B");
    FileTree<File> fileTree = TrieBasedFileTree.from(Set.of(file("A"), expected, file("A/B/C")));
    assertThat(fileTree.get(expected.relativePath())).hasValue(expected);
  }

  @Test
  void get_emptyWhenNodeAbsent() {
    File notExpected = file("A/B");
    FileTree<File> fileTree = TrieBasedFileTree.from(Set.of(file("A"), file("A/B/C")));
    assertThat(fileTree.get(notExpected.relativePath())).isEmpty();
  }

  @Test
  void contains_trueWhenNodePresent() {
    File expected = file("A/B");
    FileTree<File> fileTree = TrieBasedFileTree.from(Set.of(file("A"), expected, file("A/B/C")));
    assertThat(fileTree.contains(expected.relativePath())).isTrue();
  }

  @Test
  void contains_falseWhenNodeAbsent() {
    File notExpected = file("A/B");
    FileTree<File> fileTree = TrieBasedFileTree.from(Set.of(file("A"), file("A/B/C")));
    assertThat(fileTree.contains(notExpected.relativePath())).isFalse();
  }

  @Test
  void ancestors_returnsAncestors() {
    FileTree<File> fileTree =
        TrieBasedFileTree.from(
            Set.of(
                file("A"),
                file("A/B"),
                file("A/B/C"),
                file("A/B/C/D/E"),
                file("A/B/C/D/sibling"),
                file("A/B/C/D/E/child")));
    assertThat(fileTree.ancestors(Path.of("A/B/C/D/E")))
        .containsExactly(file("A"), file("A/B"), file("A/B/C"));
  }

  @Test
  void ancestors_emptyWhenNodeNotInTree() {
    FileTree<File> fileTree =
        TrieBasedFileTree.from(
            Set.of(
                file("A"),
                file("A/B"),
                file("A/B/C"),
                file("A/B/C/D/E"),
                file("A/B/C/D/sibling"),
                file("A/B/C/D/E/child")));
    assertThat(fileTree.ancestors(Path.of("A/B/C/D"))).isEmpty();
  }

  @Test
  void descendants_returnsDescendants() {
    FileTree<File> fileTree =
        TrieBasedFileTree.from(
            Set.of(
                file("A"),
                file("A/B"),
                file("A/sibling"),
                file("A/B/C"),
                file("A/B/C/D/E"),
                file("A/B/C/D/F")));
    assertThat(fileTree.descendants(Path.of("A/B")))
        .containsExactly(file("A/B/C"), file("A/B/C/D/E"), file("A/B/C/D/F"));
  }

  @Test
  void descendants_emptyWhenNodeNotInTree() {
    FileTree<File> fileTree =
        TrieBasedFileTree.from(
            Set.of(
                file("A"),
                file("A/B"),
                file("A/sibling"),
                file("A/B/C"),
                file("A/B/C/D/E"),
                file("A/B/C/D/F")));
    assertThat(fileTree.descendants(Path.of("A/B/C/D"))).isEmpty();
  }

  @Test
  void fileCount_countsFiles() {
    FileTree<File> fileTree =
        TrieBasedFileTree.from(
            Set.of(
                directory("A"),
                directory("A/B"),
                file("A/B/C"),
                file("A/B/D"),
                directory("X"),
                directory("X/Y"),
                file("X/Y/Z")));
    assertThat(fileTree.fileCount()).isEqualTo(3);
  }

  @Test
  void totalSize_sumsFileSize() {
    FileTree<File> fileTree =
        TrieBasedFileTree.from(
            Set.of(
                directory("A"),
                directory("A/B"),
                file("A/B/C"),
                file("A/B/D"),
                directory("X"),
                directory("X/Y"),
                file("X/Y/Z")));
    assertThat(fileTree.totalSize()).isEqualTo(6);
  }

  private static File file(String path) {
    return new TestFile(path, Path.of(path), 2, false);
  }

  private static File directory(String path) {
    return new TestFile(path, Path.of(path), 0, true);
  }

  private record TestFile(String uri, Path relativePath, long size, boolean isDirectory)
      implements File {}
}
