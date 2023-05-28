package com.willmolloy.backup;

import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Represents a {@link Location}s file tree.
 *
 * @see #builder
 * @param <FileT> type of file stored in this file tree
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public interface FileTree<FileT extends File> {

  Optional<FileT> get(Path relativePath);

  Stream<FileT> postorder();

  Stream<FileT> leaves();

  Stream<FileT> ancestors(FileT file);

  FileTree<FileT> subtree(FileT file);

  long fileCount();

  long totalSize();

  static <FileT extends File> Builder<FileT> builder(
      FileT root, DirectoryFiller<FileT> directoryFiller) {
    return new TrieBasedFileTree.Builder<>(root, directoryFiller);
  }

  /**
   * {@link FileTree} builder.
   *
   * @param <FileT> type of file stored in the built file tree
   */
  interface Builder<FileT extends File> {

    Builder<FileT> insert(FileT file);

    FileTree<FileT> build();
  }
}
