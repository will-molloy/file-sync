package com.willmolloy.backup;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

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

  boolean put(String key);

  boolean delete(String key);

  /** Backup location (source or destination). */
  interface Location {

    /** Returns map of relativized file path to file. */
    Map<String, File> scan();
  }

  /** Backup file. */
  // interface rather than record so the operations can be lazy
  interface File {

    OptionalLong size();

    Optional<Instant> lastModified();
  }
}
