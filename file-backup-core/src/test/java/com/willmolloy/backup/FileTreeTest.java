package com.willmolloy.backup;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * FileTreeTest.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
class FileTreeTest {

  @Test
  void forEach_visitsEachNodeExactlyOnce() {
    // Given
    var expected =
        Stream.of("A", "A/B", "A/B/C", "D", "D/E", "D/F", "X/Y/Z").map(s -> file(s)).toList();
    FileTree fileTree =
        FileTree.from(
            expected.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

    // When
    var actual = new ArrayList<>();
    fileTree.forEach((key, file) -> actual.add(Map.entry(key, file)));

    // Then
    assertThat(actual).containsExactlyElementsIn(expected);
  }

  @Test
  void get_presentWhenNodePresent() {
    var expected = file("A/B");
    FileTree fileTree = FileTree.from(Map.ofEntries(file("A"), expected, file("A/B/C")));
    assertThat(fileTree.get(expected.getKey())).hasValue(expected.getValue());
  }

  @Test
  void get_emptyWhenNodeAbsent() {
    var notExpected = file("A/B");
    FileTree fileTree = FileTree.from(Map.ofEntries(file("A"), file("A/B/C")));
    assertThat(fileTree.get(notExpected.getKey())).isEmpty();
  }

  @Test
  void contains_trueWhenNodePresent() {
    var expected = file("A/B");
    FileTree fileTree = FileTree.from(Map.ofEntries(file("A"), expected, file("A/B/C")));
    assertThat(fileTree.contains(expected.getKey())).isTrue();
  }

  @Test
  void contains_falseWhenNodeAbsent() {
    var notExpected = file("A/B");
    FileTree fileTree = FileTree.from(Map.ofEntries(file("A"), file("A/B/C")));
    assertThat(fileTree.contains(notExpected.getKey())).isFalse();
  }

  @Test
  void ancestors_returnsAncestors() {
    FileTree fileTree =
        FileTree.from(
            Map.ofEntries(
                file("A"),
                file("A/B"),
                file("A/B/C"),
                file("A/B/C/D/E"),
                file("A/B/C/D/sibling"),
                file("A/B/C/D/E/child")));
    assertThat(fileTree.ancestors(Path.of("A/B/C/D/E")))
        .containsExactly(Path.of("A"), Path.of("A/B"), Path.of("A/B/C"));
  }

  @Test
  void ancestors_emptyWhenNodeNotInTree() {
    FileTree fileTree =
        FileTree.from(
            Map.ofEntries(
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
    FileTree fileTree =
        FileTree.from(
            Map.ofEntries(
                file("A"),
                file("A/B"),
                file("A/sibling"),
                file("A/B/C"),
                file("A/B/C/D/E"),
                file("A/B/C/D/F")));
    assertThat(fileTree.descendants(Path.of("A/B")))
        .containsExactly(Path.of("A/B/C"), Path.of("A/B/C/D/E"), Path.of("A/B/C/D/F"));
  }

  @Test
  void descendants_emptyWhenNodeNotInTree() {
    FileTree fileTree =
        FileTree.from(
            Map.ofEntries(
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
    FileTree fileTree =
        FileTree.from(
            Map.ofEntries(
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
    FileTree fileTree =
        FileTree.from(
            Map.ofEntries(
                directory("A"),
                directory("A/B"),
                file("A/B/C"),
                file("A/B/D"),
                directory("X"),
                directory("X/Y"),
                file("X/Y/Z")));
    assertThat(fileTree.totalSize()).isEqualTo(6);
  }

  private static Map.Entry<Path, FileTree.File> file(String path) {
    FileTree.File file = mock(FileTree.File.class);
    when(file.size()).thenReturn(2L);
    return Map.entry(Path.of(path), file);
  }

  private static Map.Entry<Path, FileTree.File> directory(String path) {
    FileTree.File dir = mock(FileTree.File.class);
    when(dir.isDirectory()).thenReturn(true);
    return Map.entry(Path.of(path), dir);
  }
}
