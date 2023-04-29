package com.willmolloy.backup;

import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Backup type definition.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
interface Backup {
  // TODO possible to split into Source & Destination interfaces?

  Stream<Path> scanSource();

  Stream<Path> scanDestination();

  void tryCopyOrUpdate(Path path);

  void tryDelete(Path path);
}
