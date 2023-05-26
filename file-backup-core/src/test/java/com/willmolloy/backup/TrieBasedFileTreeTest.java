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
    TrieBasedFileTree.Builder<File> builder = TrieBasedFileTree.builder();
    expected.forEach(builder::insert);
    TrieBasedFileTree<File> fileTree = builder.build();

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
    TrieBasedFileTree.Builder<File> builder =
        TrieBasedFileTree.builder().withDirectoryFiller(directoryFiller());
    expected.forEach(builder::insert);
    TrieBasedFileTree<File> fileTree = builder.build();

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
  void contains_whenNodePresent_true() {
    File expected = file("A/B");
    TrieBasedFileTree<File> fileTree =
        TrieBasedFileTree.builder()
            .insert(file("A"))
            .insert(expected)
            .insert(file("A/B/C"))
            .build();
    assertThat(fileTree.contains(expected.relativePath())).isTrue();
  }

  @Test
  void contains_whenNodeAbsent_false() {
    File notExpected = file("A/B");
    TrieBasedFileTree<File> fileTree =
        TrieBasedFileTree.builder().insert(file("A")).insert(file("A/B/C")).build();
    assertThat(fileTree.contains(notExpected.relativePath())).isFalse();
  }

  @Test
  void contains_withDirectoryFiller_whenMissingDirFilledIn_true() {
    File expected = file("A/B");
    TrieBasedFileTree<File> fileTree =
        TrieBasedFileTree.builder()
            .withDirectoryFiller(directoryFiller())
            .insert(file("A"))
            .insert(file("A/B/C"))
            .build();
    assertThat(fileTree.contains(expected.relativePath())).isTrue();
  }

  @Test
  void contains_withDirectoryFiller_whenMissingDirNotFilledIn_false() {
    File notExpected = file("A/B/C/D");
    TrieBasedFileTree<File> fileTree =
        TrieBasedFileTree.builder()
            .withDirectoryFiller(directoryFiller())
            .insert(file("A"))
            .insert(file("A/B/C"))
            .build();
    assertThat(fileTree.contains(notExpected.relativePath())).isFalse();
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
    assertThat(fileTree.ancestors(Path.of("A/B/C/D/E")))
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
    assertThat(fileTree.ancestors(Path.of("A/B/C/D"))).isEmpty();
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
    assertThat(fileTree.ancestors(Path.of("A/B/C/D/E")))
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
    assertThat(fileTree.descendants(Path.of("A/B/C")))
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
    assertThat(fileTree.descendants(Path.of("A/B/C/D"))).isEmpty();
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
    assertThat(fileTree.descendants(Path.of("A/B/C")))
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
