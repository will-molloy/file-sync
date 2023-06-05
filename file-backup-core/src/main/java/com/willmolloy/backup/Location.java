package com.willmolloy.backup;

/**
 * Backup location (source or destination).
 *
 * @param <FileT> type of file stored in this location
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public interface Location<FileT extends File> {

  /** Scans the location's {@link FileTree}. */
  FileTree<FileT> scan();
}
