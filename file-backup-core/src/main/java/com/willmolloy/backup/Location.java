package com.willmolloy.backup;

/**
 * {@link Backup} location (source or destination).
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public interface Location {

  /** Scans the location's {@link FileTree}. */
  FileTree scan();
}
