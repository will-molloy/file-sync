package com.willmolloy.backup;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;

import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * FileTreeTest.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
class FileTreeTest {

  // no reason to test all the methods, most just delegate to the underlying map

  @Test
  void containsParentOf_trueWhenParentPresent() {
    FileTree fileTree = FileTree.from(Map.ofEntries(file("A"), file("A/B")));
    assertThat(fileTree.containsParentOf(Path.of("A/B"))).isTrue();
  }

  @Test
  void containsParentOf_falseWhenParentAbsent() {
    FileTree fileTree = FileTree.from(Map.ofEntries(file("A"), file("A/B")));
    assertThat(fileTree.containsParentOf(Path.of("A"))).isFalse();
  }

  @Test
  void containsAnyChildOf_trueWhenChildPresent() {
    FileTree fileTree = FileTree.from(Map.ofEntries(file("A"), file("A/B")));
    assertThat(fileTree.containsAnyChildOf(Path.of("A"))).isTrue();
  }

  @Test
  void containsAnyChildOf_falseWhenChildAbsent() {
    FileTree fileTree = FileTree.from(Map.ofEntries(file("A"), file("A/B")));
    assertThat(fileTree.containsAnyChildOf(Path.of("A/B"))).isFalse();
  }

  @Test
  void containsAnyChildOf_falseWhenPrefixOfNextSiblingAndChildAbsent() {
    FileTree fileTree = FileTree.from(Map.ofEntries(file("A"), file("AB")));
    assertThat(fileTree.containsAnyChildOf(Path.of("A"))).isFalse();
  }

  @Test
  void containsAnyChildOf_trueWhenPrefixOfNextSiblingAndChildPresent() {
    FileTree fileTree = FileTree.from(Map.ofEntries(file("A"), file("AB"), file("A/B")));
    assertThat(fileTree.containsAnyChildOf(Path.of("A"))).isTrue();
  }

  private static Map.Entry<Path, FileTree.File> file(String path) {
    return Map.entry(Path.of(path), mock(FileTree.File.class));
  }
}
