package com.willmolloy.backup;

import java.nio.file.Path;

/**
 * Represents a file in the {@link FileTree}.
 *
 * @see BaseFile
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public interface File {

  /** Unique identifier. */
  String uri();

  /**
   * {@linkplain Path#relativize Relative path}.
   *
   * @apiNote Enables a consistent key across different {@link Location}s.
   */
  Path relativePath();

  /** {@code true} if directory. {@code false} if regular file. */
  boolean isDirectory();

  /** File size in bytes. */
  long size();
}
