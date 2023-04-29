package com.willmolloy.backup;

import java.nio.file.Path;

/**
 * Backup type definition.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
interface Backup {
  // TODO quite a lot of methods... possible to split into Source & Destination interfaces?

  FileTree scanSource();

  FileTree scanDestination();

  void copy(Path file);

  void delete(Path file);

  void update(Path file);
}
