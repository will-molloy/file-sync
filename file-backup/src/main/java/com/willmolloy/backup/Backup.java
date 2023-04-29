package com.willmolloy.backup;

import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Backup type definition.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public interface Backup {

  Source source();

  Destination destination();

  void tryCopyOrUpdate(Path path);

  void tryDelete(Path path);

  /** Backup source. */
  interface Source {

    Path root();

    Stream<Path> scan();
  }

  /** Backup destination. */
  interface Destination {

    Path root();

    Stream<Path> scan();
  }
}
