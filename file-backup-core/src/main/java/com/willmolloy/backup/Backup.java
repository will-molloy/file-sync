package com.willmolloy.backup;

/**
 * Backup type definition.
 *
 * @param <SourceT> source location type
 * @param <DestinationT> destination location type
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public interface Backup<SourceT extends Location, DestinationT extends Location> {

  SourceT source();

  DestinationT destination();

  /**
   * Creates or updates the corresponding file on destination.
   *
   * @return {@code true} if create/update was successful
   * @implSpec Creates parent directories when necessary
   */
  boolean put(File sourceFile);

  /**
   * Deletes the file on destination.
   *
   * @return {@code true} if delete was successful
   * @implSpec Deletes children directories/files when necessary
   */
  boolean delete(File destFile);
}
