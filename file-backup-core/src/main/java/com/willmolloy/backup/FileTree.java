package com.willmolloy.backup;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

/**
 * Represents a {@link Location}s file tree.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public interface FileTree {

  /** Constructs a new {@link FileTree}; inserts each entry of the given map. */
  static FileTree from(Map<Path, ? extends File> map) {
    return TrieBasedFileTree.from(map);
  }

  void forEach(BiConsumer<Path, File> consumer);

  Optional<File> get(Path key);

  boolean contains(Path key);

  Stream<Path> ancestors(Path key);

  Stream<Path> descendants(Path key);

  long fileCount();

  long totalSize();
}
