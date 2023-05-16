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
   * @implSpec Creates parents when necessary
   */
  boolean put(Path key);

  /**
   * Deletes the file identified by {@code key} on destination.
   *
   * @return {@code true} if delete was successful
   * @implSpec Deletes children when necessary
   */
  boolean delete(Path key);

  /** Backup location (source or destination). */
  interface Location {

    /**
     * Scans the location's file tree.
     */
    FileTree scan();
  }
}
