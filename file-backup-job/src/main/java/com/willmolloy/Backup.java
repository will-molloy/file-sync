package com.willmolloy;

import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Backup type definition.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public interface Backup {
  // TODO quite a lot of methods... possible to split into Source & Destination interfaces?

  Stream<Path> scanSource();

  Stream<Path> scanDestination();

  void copy(Path file);

  void delete(Path file);

  void update(Path file);
}
