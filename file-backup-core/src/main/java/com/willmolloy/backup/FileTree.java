package com.willmolloy.backup;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Represents a {@link Location}s file tree.
 *
 * @param <FileT> type of file stored in this file tree
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public interface FileTree<FileT extends File> {

  // TODO builder?

  /** Constructs a new {@link FileTree}; containing each {@link File} from the given {@code set}. */
  static <FileT extends File> FileTree<FileT> fromSet(Set<FileT> set) {
    return TrieBasedFileTree.fromSet(set);
  }

  /**
   * Constructs a new {@link FileTree}; containing each {@link File} from the given {@code set}; and
   * missing directories filled in by the {@code directoryFiller}.
   *
   * @apiNote Useful for cases where directories are not scanned. E.g. AWS S3 ListObjects.
   */
  static <FileT extends File> FileTree<FileT> fromSetWithDirectoryFiller(
      Set<FileT> set, Function<String, FileT> directoryFiller) {
    return TrieBasedFileTree.fromSetWithDirectoryFiller(set, directoryFiller);
  }

  /** Returns an empty {@link FileTree}. */
  static <FileT extends File> FileTree<FileT> empty() {
    return TrieBasedFileTree.fromSet(Set.of());
  }

  void forEach(Consumer<FileT> consumer);

  Optional<FileT> get(Path relativePath);

  boolean contains(Path relativePath);

  Stream<FileT> ancestors(Path relativePath);

  Stream<FileT> descendants(Path relativePath);

  long fileCount();

  long totalSize();
}
