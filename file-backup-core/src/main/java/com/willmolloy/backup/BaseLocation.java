package com.willmolloy.backup;

import static com.willmolloy.backup.util.TimeHelper.elapsed;

import java.text.NumberFormat;
import java.util.Locale;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Base {@link Location} class. Caches the {@link FileTree} after scan.
 *
 * @param <FileT> type of file stored in this location
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public abstract class BaseLocation<FileT extends File> implements Location<FileT> {

  private static final Logger log = LogManager.getLogger();
  private static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance(Locale.ENGLISH);
  private static final int MEGA = 1_000_000;

  private volatile FileTree<FileT> fileTree;

  @Override
  public final FileTree<FileT> fileTree() {
    // simple double-checked locking
    if (fileTree == null) {
      synchronized (this) {
        if (fileTree == null) {
          long scanStartNanos = System.nanoTime();
          FileTree<FileT> fileTree = scan();
          log.info(
              "Scanned {} in: {}. {} files. {}MB",
              this,
              elapsed(scanStartNanos),
              NUMBER_FORMAT.format(fileTree.fileCount()),
              NUMBER_FORMAT.format(fileTree.totalSize() / MEGA));
          this.fileTree = fileTree;
        }
      }
    }
    return fileTree;
  }

  protected abstract FileTree<FileT> scan();
}
