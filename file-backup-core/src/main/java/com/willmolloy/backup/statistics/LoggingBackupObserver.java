package com.willmolloy.backup.statistics;

import com.willmolloy.backup.Backup;
import com.willmolloy.backup.FileTree;
import com.willmolloy.backup.Location;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.Locale;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * {@link BackupObserver} which simply logs.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public final class LoggingBackupObserver implements BackupObserver {
  private static final Logger log = LogManager.getLogger();

  private static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance(Locale.ENGLISH);
  private static final int MEGA = 1_000_000;

  @Override
  public void notifyStarted(Backup<?, ?> backup) {
    log.info("Started: {}", backup);
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
  public void notifyFinished(Backup<?, ?> backup, Statistics.Snapshot stats, Duration elapsed) {
    log.info(
        "Finished: {} in: {}. {} files created, {} files updated, {} files deleted, {} files same. {}MB added, {}MB removed",
        backup,
        elapsed,
        NUMBER_FORMAT.format(stats.creates()),
        NUMBER_FORMAT.format(stats.updates()),
        NUMBER_FORMAT.format(stats.deletes()),
        NUMBER_FORMAT.format(stats.same()),
        NUMBER_FORMAT.format(stats.bytesAdded() / MEGA),
        NUMBER_FORMAT.format(stats.bytesRemoved() / MEGA));
    if (!stats.noErrors()) {
      log.warn(
          "Failed: {} creates, {} updates, {} deletes",
          NUMBER_FORMAT.format(stats.failedCreates()),
          NUMBER_FORMAT.format(stats.failedUpdates()),
          NUMBER_FORMAT.format(stats.failedDeletes()));
    }
  }

  @Override
  public void notifyFailed(Backup<?, ?> backup, Throwable t) {
    log.fatal("Fatal error", t);
  }
}
