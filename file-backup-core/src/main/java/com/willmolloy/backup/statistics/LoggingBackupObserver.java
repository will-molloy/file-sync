package com.willmolloy.backup.statistics;

import static com.willmolloy.backup.util.TimeHelper.elapsed;

import com.willmolloy.backup.BaseBackup;
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
  public void notifyStarted(BaseBackup<?, ?> backup) {
    log.info("Started: {}", backup);
  }

  @Override
  public void notifyScanned(Location<?> location, FileTree<?> fileTree, Duration elapsed) {
    log.info(
        "Scanned: {} in: {}. {} files ({}MB)",
        location,
        elapsed,
        NUMBER_FORMAT.format(fileTree.leafCount()),
        NUMBER_FORMAT.format(fileTree.totalSize() / MEGA));
  }

  @Override
  public void notifyFinished(BaseBackup<?, ?> backup, Statistics.Snapshot statistics) {
    log.info(
        "Finished: {} in: {}. {} files put ({}MB), {} files deleted ({}MB)",
        backup,
        statistics.elapsed(),
        statistics.puts(),
        NUMBER_FORMAT.format(statistics.bytesAdded() / MEGA),
        statistics.deletes(),
        NUMBER_FORMAT.format(statistics.bytesRemoved() / MEGA));
    if (!statistics.allSuccess()) {
      log.warn(
          "Failed: {} puts and {} deletes", statistics.failedPuts(), statistics.failedDeletes());
    }
  }
}
