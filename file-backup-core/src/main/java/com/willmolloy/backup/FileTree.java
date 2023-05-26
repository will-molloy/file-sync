package com.willmolloy.backup;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Represents a {@link Location}s file tree.
 *
 * @see #builder()
 * @param <FileT> type of file stored in this file tree
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public interface FileTree<FileT extends File> {

  void forEach(Consumer<FileT> consumer);

  Optional<FileT> get(Path relativePath);

  boolean contains(Path relativePath);

  Stream<FileT> ancestors(Path relativePath);

  Stream<FileT> descendants(Path relativePath);

  long fileCount();

  long totalSize();

  static <FileT extends File> Builder<FileT> builder() {
    return TrieBasedFileTree.builder();
  }

  /**
   * {@link FileTree} builder.
   *
   * @param <FileT> type of file stored in the built file tree
   */
  interface Builder<FileT extends File> {

    /**
     * Fills in missing directories with the {@code directoryFiller}.
     *
     * @apiNote Useful for cases where directories are not scanned. E.g. AWS S3 ListObjects.
     */
    Builder<FileT> withDirectoryFiller(Function<String, FileT> directoryFiller);

    Builder<FileT> insert(FileT file);

    FileTree<FileT> build();
  }
}
