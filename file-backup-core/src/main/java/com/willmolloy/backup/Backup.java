package com.willmolloy.backup;

import java.util.Optional;

/**
 * Backup type definition.
 *
 * @param <SourceFileT> source file type
 * @param <DestFileT> destination file type
 * @see BaseBackup
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public interface Backup<SourceFileT extends File, DestFileT extends File> {

  Location<SourceFileT> source();

  Location<DestFileT> destination();

  /**
   * Creates or updates the corresponding file on destination.
   *
   * @return {@code true} if create/update was successful
   * @implSpec Creates parent directories when necessary
   */
  boolean put(SourceFileT sourceFile);

  /**
   * Deletes the file on destination.
   *
   * @return {@code true} if delete was successful
   * @implSpec Deletes child directories/files when necessary
   */
  boolean delete(DestFileT destFile);

  /** {@code true} if {@link #put} is necessary. */
  default boolean needPut(SourceFileT sourceFile) {
    FileTree<SourceFileT> sourceFileTree = source().fileTree();
    if (sourceFileTree.isRoot(sourceFile)) {
      return false;
    }

    FileTree<DestFileT> destFileTree = destination().fileTree();
    Optional<DestFileT> maybeDestFile = destFileTree.get(sourceFile.relativePath());
    // either file not on dest -> create
    // OR files different -> update
    return maybeDestFile.isEmpty() || !sourceFile.same(maybeDestFile.get());
  }

  /** {@code true} if {@link #delete} is necessary. */
  default boolean needDelete(DestFileT destFile) {
    FileTree<DestFileT> destFileTree = destination().fileTree();
    // don't delete the root, it was created manually outside this app; if it's deleted subsequent
    // runs will fail
    if (destFileTree.isRoot(destFile)) {
      return false;
    }

    FileTree<SourceFileT> sourceFileTree = source().fileTree();
    // file not on source -> delete
    return !sourceFileTree.contains(destFile.relativePath());
  }

  /** Runs the backup. */
  default boolean run() {
    return new BackupRunner<>(this).run();
  }
}
