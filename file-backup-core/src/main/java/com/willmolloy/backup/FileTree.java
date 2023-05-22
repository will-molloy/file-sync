package com.willmolloy.backup;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Represents a {@link Location}s file tree.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public interface FileTree {

  // TODO generic File

  /** Constructs a new {@link FileTree}; containing each {@link File} from the given set. */
  static FileTree from(Set<? extends File> set) {
    return TrieBasedFileTree.from(set);
  }

  static FileTree empty() {
    return FileTree.from(Set.of());
  }

  void forEach(Consumer<File> consumer);

  Optional<File> get(Path relativePath);

  boolean contains(Path relativePath);

  Stream<File> ancestors(Path relativePath);

  Stream<File> descendants(Path relativePath);

  long fileCount();

  long totalSize();
}
