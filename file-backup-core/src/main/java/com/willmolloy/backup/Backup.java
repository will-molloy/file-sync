package com.willmolloy.backup;

/**
 * Backup type definition.
 *
 * @param <SourceFileT> source file type
 * @param <DestFileT> destination file type
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
   * @implSpec Deletes children directories/files when necessary
   */
  boolean delete(DestFileT destFile);
}
