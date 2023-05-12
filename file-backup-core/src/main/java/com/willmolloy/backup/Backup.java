package com.willmolloy.backup;

import java.util.Map;

/**
 * Backup type definition.
 *
 * @param <SourceT> source location type
 * @param <DestinationT> destination location type
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public interface Backup<
    SourceT extends Backup.Location<?>, DestinationT extends Backup.Location<?>> {

  SourceT source();

  DestinationT destination();

  /**
   * Creates or updates the file identified by {@code key} on destination with the corresponding
   * file on source.
   *
   * @return {@code true} if create/update was successful
   */
  boolean put(String key);

  /**
   * Deletes the file identified by {@code key} on destination.
   *
   * @return {@code true} if delete was successful
   */
  boolean delete(String key);

  /**
   * Backup location (source or destination).
   *
   * @param <FileT> type of file stored on this location
   */
  interface Location<FileT extends File> {

    /**
     * Scans the location.
     *
     * @return Map of relativized file path (key) to file.
     */
    Map<String, FileT> scan();
  }

  /** Backup file. */
  interface File {

    /** File size in bytes. */
    long size();

    /** {@code true} if file. {@code false} if directory. */
    boolean isRegularFile();

    /**
     * {@code true} if the {@code other} file can be considered the same.
     *
     * @apiNote Used to determine if a file requires updating.
     * @implNote The default implementation just looks at file size.
     */
    // for s3; considered last-modified, but it's really object-creation time.
    // also considered e-tag, but it's calculated differently for large (> 16MB) files.
    // file size is good enough?
    default boolean same(File other) {
      return size() == other.size();
    }
  }
}
