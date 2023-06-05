package com.willmolloy.backup.statistics;

import com.willmolloy.backup.File;
import com.willmolloy.backup.FileTree;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

/**
 * Backup statistics.
 *
 * @param <SourceFileT> source file type
 * @param <DestFileT> destination file type
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public final class Statistics<SourceFileT extends File, DestFileT extends File> {

  private final AtomicInteger creates = new AtomicInteger();
  private final AtomicInteger updates = new AtomicInteger();
  private final AtomicInteger deletes = new AtomicInteger();
  private final AtomicInteger same = new AtomicInteger();
  private final AtomicInteger failedCreates = new AtomicInteger();
  private final AtomicInteger failedUpdates = new AtomicInteger();
  private final AtomicInteger failedDeletes = new AtomicInteger();
  private final AtomicLong bytesAdded = new AtomicLong();
  private final AtomicLong bytesRemoved = new AtomicLong();

  public void countCreate(SourceFileT sourceFile) {
    creates.incrementAndGet();
    bytesAdded.addAndGet(sourceFile.size());
  }

  public void countFailedCreate(SourceFileT file) {
    failedCreates.incrementAndGet();
  }

  /** Count update. */
  public void countUpdate(SourceFileT sourceFile, DestFileT destFile) {
    updates.incrementAndGet();
    bytesAdded.addAndGet(sourceFile.size());
    bytesRemoved.addAndGet(destFile.size());
  }

  public void countFailedUpdate(SourceFileT sourceFile, DestFileT destFile) {
    failedUpdates.incrementAndGet();
  }

  public void countDelete(FileTree<DestFileT> subtree) {
    deletes.addAndGet((int) subtree.leafCount());
    bytesRemoved.addAndGet(subtree.totalSize());
  }

  public void countFailedDelete(FileTree<DestFileT> subtree) {
    failedDeletes.addAndGet((int) subtree.leafCount());
  }

  public void countSame() {
    same.incrementAndGet();
  }

  /** Get snapshot of current backup statistics. */
  public Snapshot snapshot() {
    return new Snapshot(
        creates.get(),
        updates.get(),
        deletes.get(),
        same.get(),
        failedCreates.get(),
        failedUpdates.get(),
        failedDeletes.get(),
        bytesAdded.get(),
        bytesRemoved.get());
  }

  /**
   * Snapshot of current backup statistics.
   *
   * @param creates count of files created
   * @param updates count of files updated
   * @param deletes count of files deleted
   * @param same count of files that remained the same
   * @param failedCreates failed creates
   * @param failedUpdates failed updates
   * @param failedDeletes failed deletes
   * @param bytesAdded bytes added
   * @param bytesRemoved bytes removed
   */
  public record Snapshot(
      int creates,
      int updates,
      int deletes,
      int same,
      int failedCreates,
      int failedUpdates,
      int failedDeletes,
      long bytesAdded,
      long bytesRemoved) {

    public boolean allSuccess() {
      return IntStream.of(failedCreates, failedUpdates, failedDeletes).allMatch(i -> i == 0);
    }
  }
}
