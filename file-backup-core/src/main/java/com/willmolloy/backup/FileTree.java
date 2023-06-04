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
public sealed interface FileTree<FileT extends File> permits FileTreeNode {

  /** Lookup by {@link File#relativePath()}. */
  Optional<FileT> get(Path relativePath);

  /** Lookup test by {@link File#relativePath()}. */
  default boolean contains(Path relativePath) {
    return get(relativePath).isPresent();
  }

  /** Traverses all nodes in a post-order manner. */
  Stream<FileT> postorder();

  /** Traverses all leaves, left to right. */
  Stream<FileT> leaves();

  /** Traverses ancestors from the parent of the given {@code file} to the root. */
  Stream<FileT> ancestors(FileT file);

  /** Returns the subtree rooted at the given {@code file}. */
  FileTree<FileT> subtree(FileT file);

  /** Count of leaves. */
  long leafCount();

  /**
   * Total size in bytes.
   *
   * @see File#size()
   */
  long totalSize();

  /**
   * Returns an instance of {@link FileTree.Builder}.
   *
   * @param root file which will be the root of the {@link FileTree}
   */
  static <FileT extends File> Builder<FileT> builder(FileT root) {
    return builder(
        root,
        path -> {
          throw new IllegalStateException("Directory Filler unexpected");
        });
  }

  /**
   * Returns an instance of {@link FileTree.Builder}.
   *
   * @param root file which will be the root of the {@link FileTree}
   * @param directoryFiller {@link DirectoryFiller} to fill in missing directories during {@link
   *     FileTree.Builder#insert}.
   */
  static <FileT extends File> Builder<FileT> builder(
      FileT root, DirectoryFiller<FileT> directoryFiller) {
    return new FileTreeNode.Builder<>(root, directoryFiller);
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
