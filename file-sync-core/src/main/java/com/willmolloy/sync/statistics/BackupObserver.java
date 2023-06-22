package com.willmolloy.sync.statistics;

import com.willmolloy.sync.Backup;
import com.willmolloy.sync.FileTree;
import com.willmolloy.sync.Location;
import java.time.Duration;

/**
 * Observes a backup run.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public interface BackupObserver {

  void notifyStarted(Backup<?, ?> backup);

  void notifyScanned(Location<?> location, FileTree<?> fileTree, Duration elapsed);

  void notifyFinished(Backup<?, ?> backup, Statistics.Snapshot stats, Duration elapsed);

  void notifyFailed(Backup<?, ?> backup, Throwable t);
}
