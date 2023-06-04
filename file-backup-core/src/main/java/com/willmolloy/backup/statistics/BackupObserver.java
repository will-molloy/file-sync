package com.willmolloy.backup.statistics;

import com.willmolloy.backup.BaseBackup;

/**
 * Observes a backup run.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public interface BackupObserver {

  // TODO notifyScanned?

  void notifyStarted(BaseBackup<?, ?> backup);

  void notifyFinished(BaseBackup<?, ?> backup, Statistics.Snapshot statistics);
}
