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

  /**
   * {@code true} if the {@code other} file can be considered the same.
   *
   * @apiNote Used to determine if a file requires updating.
   * @implNote The default implementation just looks at file size.
   */
  default boolean same(File other) {
    // for s3; considered last-modified, but it's really object-creation time.
    // also considered e-tag, but it's calculated differently for large (> 16MB) files.
    // file size is good enough?
    return isDirectory() == other.isDirectory() && size() == other.size();
  }
}
