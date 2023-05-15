package com.willmolloy.backup;

import java.util.Map;

/**
 * Backup type definition.
 *
 * @param <SourceT> source location type
 * @param <DestinationT> destination location type
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public interface Backup<SourceT extends Backup.Location, DestinationT extends Backup.Location> {

  SourceT source();

  DestinationT destination();

  /**
   * Creates or updates the file identified by {@code key} on destination with the corresponding
   * file on source.
   *
   * @return {@code true} if create/update was successful
   * @implSpec Must create parents as required
   */
  boolean put(String key);

  /**
   * Deletes the file identified by {@code key} on destination.
   *
   * @return {@code true} if delete was successful
   * @implSpec Must delete children as required
   */
  boolean delete(String key);

  /** Backup location (source or destination). */
  interface Location {

    /**
     * Scans the location's file tree.
     *
     * @return Map of (relativized) paths to nodes.
     */
    Map<String, Node> scan();
  }

  /** Represents a node in a locations file tree. Either a file or directory. */
  sealed interface Node permits Node.File, Node.Directory {

    /**
     * {@code true} if the {@code other} file can be considered the same.
     *
     * @apiNote Used to determine if a file requires updating.
     * @implNote The default implementation just looks at file size.
     */
    boolean same(Node other);

    /** File. */
    non-sealed interface File extends Node {

      /** File size in bytes. */
      long size();

      /**
       * {@code true} if the {@code other} file can be considered the same.
       *
       * @apiNote Used to determine if a file requires updating.
       * @implNote The default implementation just looks at file size.
       */
      // for s3; considered last-modified, but it's really object-creation time.
      // also considered e-tag, but it's calculated differently for large (> 16MB) files.
      // file size is good enough?
      @Override
      default boolean same(Node other) {
        return other instanceof File file && size() == file.size();
      }
    }

    /** Directory. */
    non-sealed interface Directory extends Node {}
  }
}
