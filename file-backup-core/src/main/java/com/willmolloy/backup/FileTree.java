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

  Optional<File> get(Path path);

  boolean contains(Path path);

  Stream<Path> ancestors(Path path);

  Stream<Path> descendants(Path path);

  long fileCount();

  long totalSize();
}
