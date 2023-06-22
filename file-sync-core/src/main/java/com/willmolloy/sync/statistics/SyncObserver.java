package com.willmolloy.sync.statistics;

import com.willmolloy.sync.FileTree;
import com.willmolloy.sync.Location;
import com.willmolloy.sync.Sync;
import java.time.Duration;

/**
 * Observes a sync.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public interface SyncObserver {

  void notifyStarted(Sync<?, ?> sync);

  void notifyScanned(Location<?> location, FileTree<?> fileTree, Duration elapsed);

  void notifyFinished(Sync<?, ?> sync, Statistics.Snapshot stats, Duration elapsed);

  void notifyFailed(Sync<?, ?> sync, Throwable t);
}
