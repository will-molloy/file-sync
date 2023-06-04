package com.willmolloy.backup.statistics;

import static com.willmolloy.backup.util.TimeHelper.elapsed;

import com.willmolloy.backup.File;
import com.willmolloy.backup.FileTree;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

/**
 * Backup statistics.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public final class Statistics {

  private final AtomicInteger puts = new AtomicInteger();
  private final AtomicInteger deletes = new AtomicInteger();
  private final AtomicInteger failedPuts = new AtomicInteger();
  private final AtomicInteger failedDeletes = new AtomicInteger();
  private final AtomicLong bytesAdded = new AtomicLong();
  private final AtomicLong bytesRemoved = new AtomicLong();
  private final long startNanos = System.nanoTime();

  /** Count put. */
  public void recordPut(File file) {
    puts.incrementAndGet();
    // TODO doesn't account for bytes removed by update... need to distinct create/update?
    //  ^ shouldn't do it for cases where delete occurred before create, only pure update
    bytesAdded.addAndGet(file.size());
  }

  public void recordFailedPut(File file) {
    failedPuts.incrementAndGet();
  }

  public void recordDelete(FileTree<?> fileTree) {
    deletes.addAndGet((int) fileTree.leafCount());
    bytesRemoved.addAndGet(fileTree.totalSize());
  }

  public void recordFailedDelete(FileTree<?> fileTree) {
    failedDeletes.addAndGet((int) fileTree.leafCount());
  }

  /** Get snapshot of current backup statistics. */
  public Snapshot snapshot() {
    return new Snapshot(
        puts.get(),
        deletes.get(),
        failedPuts.get(),
        failedDeletes.get(),
        bytesAdded.get(),
        bytesRemoved.get(),
        elapsed(startNanos));
  }

  /**
   * Snapshot of current backup statistics.
   *
   * @param puts put count
   * @param deletes delete count
   * @param failedPuts failed put count
   * @param failedDeletes failed delete count
   * @param bytesAdded bytes added
   * @param bytesRemoved bytes removed
   * @param elapsed elapsed duration
   */
  public record Snapshot(
      int puts,
      int deletes,
      int failedPuts,
      int failedDeletes,
      long bytesAdded,
      long bytesRemoved,
      Duration elapsed) {

    public boolean allSuccess() {
      return IntStream.of(failedPuts, failedDeletes).allMatch(i -> i == 0);
    }
  }
}
