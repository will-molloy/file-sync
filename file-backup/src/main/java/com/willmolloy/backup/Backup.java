package com.willmolloy.backup;

import java.time.Instant;
import java.util.Map;

/**
 * Backup type definition.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public interface Backup {

  Location source();

  Location destination();

  void copy(String key);

  void update(String key);

  void delete(String key);

  /** Backup location (source or destination). */
  interface Location {

    Map<String, File> scan();
  }

  /** Backup file. */
  // interface rather than record so the operations can be lazy
  interface File {

    long size();

    Instant lastModified();
  }
}
