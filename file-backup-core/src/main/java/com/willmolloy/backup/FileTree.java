package com.willmolloy.backup;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Represents a {@link Location}s file tree.
 *
 * @param <FileT> type of file stored in this file tree
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public interface FileTree<FileT extends File> {

  /** Constructs a new {@link FileTree}; containing each {@link File} from the given set. */
  static <FileT extends File> FileTree<FileT> from(Set<FileT> set) {
    return TrieBasedFileTree.from(set);
  }

  static <FileT extends File> FileTree<FileT> empty() {
    return FileTree.from(Set.of());
  }

  void forEach(Consumer<FileT> consumer);

  Optional<FileT> get(Path relativePath);

  boolean contains(Path relativePath);

  Stream<FileT> ancestors(Path relativePath);

  Stream<FileT> descendants(Path relativePath);

  long fileCount();

  long totalSize();
}
