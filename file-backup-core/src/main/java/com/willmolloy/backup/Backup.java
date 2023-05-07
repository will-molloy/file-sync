package com.willmolloy.backup;

import java.time.Instant;
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
   */
  boolean put(String key);

  /**
   * Deletes the file identified by {@code key} on destination.
   *
   * @return {@code true} if delete was successful
   */
  boolean delete(String key);

  /** Backup location (source or destination). */
  interface Location {

    /**
     * Scans the location.
     *
     * @return Map of relativized file path (key) to file.
     */
    Map<String, File> scan();
  }

  /** Backup file. */
  // interface rather than record so the operations can be lazy
  interface File {

    /** File size in bytes. */
    long size();

    /** Last modified time. */
    Instant lastModified();

    /**
     * Gets (or computes) the files ETag.
     *
     * @implSpec ETags are required to be wrapped in {@code "} quotes.
     * @implNote Currently implemented as MD5 digest of file contents in base16.
     */
    String etag();

    /**
     * {@code true} if the {@code other} file is considered equal.
     *
     * @apiNote Used to determine if a file requires updating.
     */
    boolean equal(File other);
  }
}
