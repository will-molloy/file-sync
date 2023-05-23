package com.willmolloy.backup;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

import com.google.common.collect.Sets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
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
    FileTree<File> fileTree = TrieBasedFileTree.fromSet(expected);

    // When
    List<File> actual = new ArrayList<>();
    fileTree.forEach(actual::add);

    // Then
    assertThat(actual).containsExactlyElementsIn(expected);
  }

  @Test
  void forEach_withDirectoryFiller_visitsEachNodeExactlyOnceIncludingMissingDirs() {
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
    FileTree<File> fileTree =
        TrieBasedFileTree.fromSetWithDirectoryFiller(expected, directoryFiller());

    // When
    List<File> actual = new ArrayList<>();
    fileTree.forEach(actual::add);

    // Then
    Set<File> missingDirs = Set.of(directory(""), directory("X"), directory("X/Y"));
    assertThat(actual).containsExactlyElementsIn(Sets.union(expected, missingDirs));
  }

  @Test
  void get_whenNodePresent_present() {
    File expected = file("A/B");
    FileTree<File> fileTree = TrieBasedFileTree.fromSet(Set.of(file("A"), expected, file("A/B/C")));
    assertThat(fileTree.get(expected.relativePath())).hasValue(expected);
  }

  @Test
  void get_whenNodeAbsent_empty() {
    File notExpected = file("A/B");
    FileTree<File> fileTree = TrieBasedFileTree.fromSet(Set.of(file("A"), file("A/B/C")));
    assertThat(fileTree.get(notExpected.relativePath())).isEmpty();
  }

  @Test
  void get_withDirectoryFiller_whenMissingDirFilledIn_present() {
    File expected = file("A/B");
    FileTree<File> fileTree =
        TrieBasedFileTree.fromSetWithDirectoryFiller(
            Set.of(file("A"), file("A/B/C")), directoryFiller());
    assertThat(fileTree.get(expected.relativePath())).hasValue(directory("A/B"));
  }

  @Test
  void get_withDirectoryFiller_whenMissingDirNotFilledIn_empty() {
    File notExpected = file("A/B/C/D");
    FileTree<File> fileTree =
        TrieBasedFileTree.fromSetWithDirectoryFiller(
            Set.of(file("A"), file("A/B/C")), directoryFiller());
    assertThat(fileTree.get(notExpected.relativePath())).isEmpty();
  }

  @Test
  void contains_whenNodePresent_true() {
    File expected = file("A/B");
    FileTree<File> fileTree = TrieBasedFileTree.fromSet(Set.of(file("A"), expected, file("A/B/C")));
    assertThat(fileTree.contains(expected.relativePath())).isTrue();
  }

  @Test
  void contains_whenNodeAbsent_false() {
    File notExpected = file("A/B");
    FileTree<File> fileTree = TrieBasedFileTree.fromSet(Set.of(file("A"), file("A/B/C")));
    assertThat(fileTree.contains(notExpected.relativePath())).isFalse();
  }

  @Test
  void contains_withDirectoryFiller_whenMissingDirFilledIn_true() {
    File expected = file("A/B");
    FileTree<File> fileTree =
        TrieBasedFileTree.fromSetWithDirectoryFiller(
            Set.of(file("A"), file("A/B/C")), directoryFiller());
    assertThat(fileTree.contains(expected.relativePath())).isTrue();
  }

  @Test
  void contains_withDirectoryFiller_whenMissingDirNotFilledIn_false() {
    File notExpected = file("A/B/C/D");
    FileTree<File> fileTree =
        TrieBasedFileTree.fromSetWithDirectoryFiller(
            Set.of(file("A"), file("A/B/C")), directoryFiller());
    assertThat(fileTree.contains(notExpected.relativePath())).isFalse();
  }

  @Test
  void ancestors_returnsAncestors() {
    FileTree<File> fileTree =
        TrieBasedFileTree.fromSet(
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
  void ancestors_whenNodeNotInTree_empty() {
    FileTree<File> fileTree =
        TrieBasedFileTree.fromSet(
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
  void ancestors_withDirectoryFiller_returnsAncestorsIncludingMissingDirs() {
    FileTree<File> fileTree =
        TrieBasedFileTree.fromSetWithDirectoryFiller(
            Set.of(
                file("A"),
                file("A/B/C"),
                file("A/B/C/D/E"),
                file("A/B/C/D/sibling"),
                file("A/B/C/D/E/child")),
            directoryFiller());
    assertThat(fileTree.ancestors(Path.of("A/B/C/D")))
        .containsExactly(file("A"), directory("A/B"), file("A/B/C"));
  }

  @Test
  void descendants_returnsDescendants() {
    FileTree<File> fileTree =
        TrieBasedFileTree.fromSet(
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
  void descendants_whenNodeNotInTree_empty() {
    FileTree<File> fileTree =
        TrieBasedFileTree.fromSet(
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
  void descendants_withDirectoryFiller_returnsDescendantsIncludingMissingDirs() {
    FileTree<File> fileTree =
        TrieBasedFileTree.fromSetWithDirectoryFiller(
            Set.of(
                file("A"),
                file("A/B"),
                file("A/sibling"),
                file("A/B/C"),
                file("A/B/C/D/E"),
                file("A/B/C/D/F"),
                file("A/B/C/D/F/X/Y/Z")),
            directoryFiller());
    assertThat(fileTree.descendants(Path.of("A/B/C/D")))
        .containsExactly(
            file("A/B/C/D/E"),
            file("A/B/C/D/F"),
            directory("A/B/C/D/F/X"),
            directory("A/B/C/D/F/X/Y"),
            file("A/B/C/D/F/X/Y/Z"));
  }

  @Test
  void fileCount_countsFiles() {
    FileTree<File> fileTree =
        TrieBasedFileTree.fromSet(
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
        TrieBasedFileTree.fromSet(
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

  private static Function<String, File> directoryFiller() {
    return path -> directory(path);
  }

  private record TestFile(String uri, Path relativePath, long size, boolean isDirectory)
      implements File {}
}
