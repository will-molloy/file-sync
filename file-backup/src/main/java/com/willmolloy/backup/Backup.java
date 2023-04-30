package com.willmolloy.backup;

import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Backup type definition.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public interface Backup {

  Location source();

  Location destination();

  void copy(Path relativePath);

  void update(Path relativePath);

  void delete(Path relativePath);

  /** Backup location (source or destination). */
  interface Location {

    // TODO hide this?
    Path root();

    Stream<Path> scan();

    boolean exists(Path relativePath);

    boolean isDirectory(Path relativePath);

    long size(Path relativePath);

    long lastModified(Path relativePath);
  }
}
