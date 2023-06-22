package com.willmolloy.sync.statistics;

import com.willmolloy.sync.FileTree;
import com.willmolloy.sync.Location;
import com.willmolloy.sync.Sync;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.Locale;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * {@link SyncObserver} which simply logs.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public final class LoggingSyncObserver implements SyncObserver {
  private static final Logger log = LogManager.getLogger();

  private static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance(Locale.ENGLISH);
  private static final int MEGA = 1_000_000;

  @Override
  public void notifyStarted(Sync<?, ?> sync) {
    log.info("Started: {}", sync);
  }

  @Override
  public void notifyScanned(Location<?> location, FileTree<?> fileTree, Duration elapsed) {
    log.info(
        "Scanned: {} in: {}. {} files. {}MB",
        location,
        elapsed,
        NUMBER_FORMAT.format(fileTree.leafCount()),
        NUMBER_FORMAT.format(fileTree.totalSize() / MEGA));
  }

  @Override
  public void notifyFinished(Sync<?, ?> sync, Statistics.Snapshot stats, Duration elapsed) {
    log.info(
        "Finished: {} in: {}. {} files created, {} files updated, {} files deleted, {} files same. {}MB added, {}MB removed",
        sync,
        elapsed,
        NUMBER_FORMAT.format(stats.creates()),
        NUMBER_FORMAT.format(stats.updates()),
        NUMBER_FORMAT.format(stats.deletes()),
        NUMBER_FORMAT.format(stats.same()),
        NUMBER_FORMAT.format(stats.bytesAdded() / MEGA),
        NUMBER_FORMAT.format(stats.bytesRemoved() / MEGA));
    if (stats.anyErrors()) {
      log.warn(
          "Failed: {} creates, {} updates, {} deletes",
          NUMBER_FORMAT.format(stats.failedCreates()),
          NUMBER_FORMAT.format(stats.failedUpdates()),
          NUMBER_FORMAT.format(stats.failedDeletes()));
    }
  }

  @Override
  public void notifyFailed(Sync<?, ?> sync, Throwable t) {
    log.fatal("Fatal error", t);
  }
}
