package com.willmolloy.backup.statistics;

import com.willmolloy.backup.BaseBackup;
import com.willmolloy.backup.FileTree;
import com.willmolloy.backup.Location;
import java.time.Duration;

/**
 * Observes a backup run.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public interface BackupObserver {

  void notifyStarted(BaseBackup<?, ?> backup);

  void notifyScanned(Location<?> location, FileTree<?> fileTree, Duration elapsed);

  void notifyFinished(BaseBackup<?, ?> backup, Statistics.Snapshot statistics);
}
