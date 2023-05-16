package com.willmolloy.backup;

import java.nio.file.Path;

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
  boolean put(Path key);

  /**
   * Deletes the file identified by {@code key} on destination.
   *
   * @return {@code true} if delete was successful
   * @implSpec Must delete children as required
   */
  boolean delete(Path key);

  /** Backup location (source or destination). */
  interface Location {

    /**
     * Scans the location's file tree.
     *
     * @return Map of (relativized) paths to nodes.
     */
    // TODO class wrapping the map
    FileTree scan();
  }
}
