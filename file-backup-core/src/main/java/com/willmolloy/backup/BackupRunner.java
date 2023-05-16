package com.willmolloy.backup;

import static com.willmolloy.backup.util.Preconditions.require;
import static java.util.Objects.requireNonNull;

import com.willmolloy.backup.FileTree.Node;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Runs a {@link Backup}.
 *
 * @see #run(Backup)
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
@SuppressFBWarnings(
    value = "RV_RETURN_VALUE_IGNORED_BAD_PRACTICE",
    justification = "Relying on default ExecutorService.close to wait for futures")
public final class BackupRunner {

  private static final Logger log = LogManager.getLogger();

  private static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance(Locale.ENGLISH);
  private static final int MEGA = 1_000_000;

  /** Runs the backup. */
  // static factory ensures single instance per run
  public static OverallStatistics run(Backup<?, ?> backup) {
    return new BackupRunner(backup).run();
  }

  private final AtomicInteger createCount = new AtomicInteger();
  private final AtomicInteger failedCreateCount = new AtomicInteger();
  private final AtomicInteger updateCount = new AtomicInteger();
  private final AtomicInteger failedUpdateCount = new AtomicInteger();
  private final AtomicInteger deleteCount = new AtomicInteger();
  private final AtomicInteger failedDeleteCount = new AtomicInteger();
  private final AtomicInteger sameCount = new AtomicInteger();
  // long can represent up to 9.2 EB
  private final AtomicLong bytesAdded = new AtomicLong();
  private final AtomicLong bytesRemoved = new AtomicLong();

  private final Backup<?, ?> backup;

  private BackupRunner(Backup<?, ?> backup) {
    this.backup = requireNonNull(backup);
  }

  private OverallStatistics run() {
    log.info("Running: {}", backup);
    long runStartNanos = System.nanoTime();

    try (ExecutorService threadPool =
        Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("worker-", 1).factory())) {

      FileTree sourceFiles = scanWithLog(backup.source()::scan, "source");
      FileTree destFiles = scanWithLog(backup.destination()::scan, "destination");

      sourceFiles.forEach(
          (key, sourceNode) -> {
            if (sourceFiles.containsAnyChildOf(key)) {
              log.debug("Skipping put({}). Covered by child", key);
              return;
            }

            Node destNode = destFiles.get(key);
            if (destNode == null) {
              log.debug("create({})", key);
              threadPool.submit(() -> create(key, sourceNode));
            } else if (!sourceNode.same(destNode)) {
              log.debug("update({})", key);
              threadPool.submit(() -> update(key, sourceNode, destNode));
            } else {
              log.debug("same({})", key);
              sameCount.incrementAndGet();
            }
          });

      destFiles.forEach(
          (key, destFile) -> {
            if (sourceFiles.contains(key)) {
              return;
            }

            // TODO test coverage for this... only skip if parent NOT in source
            if (destFiles.containsParentOf(key) && !sourceFiles.containsParentOf(key)) {
              log.debug("Skipping delete({}). Covered by parent", key);
              return;
            }

            log.debug("delete({})", key);
            threadPool.submit(() -> delete(key, destFile));
          });
    }
    return getAndLogStats(runStartNanos);
  }

  private FileTree scanWithLog(Supplier<FileTree> scan, String locationForLog) {
    long scanStartNanos = System.nanoTime();

    FileTree fileTree = scan.get();
    log.info(
        "Scanned {} in: {}. {} files. {}MB",
        locationForLog,
        elapsed(scanStartNanos),
        NUMBER_FORMAT.format(fileTree.fileCount()),
        NUMBER_FORMAT.format(fileTree.totalSize() / MEGA));

    return fileTree;
  }

  private void create(Path key, Node sourceNode) {
    if (backup.put(key)) {
      createCount.incrementAndGet();
      if (sourceNode instanceof Node.File sourceFile) {
        bytesAdded.addAndGet(sourceFile.size());
      }
    } else {
      failedCreateCount.incrementAndGet();
    }
  }

  private void update(Path key, Node sourceNode, Node destNode) {
    if (backup.put(key)) {
      updateCount.incrementAndGet();
      if (sourceNode instanceof Node.File sourceFile) {
        bytesAdded.addAndGet(sourceFile.size());
      }
      if (destNode instanceof Node.File destFile) {
        bytesRemoved.addAndGet(destFile.size());
      }
    } else {
      failedUpdateCount.incrementAndGet();
    }
  }

  private void delete(Path key, Node destNode) {
    if (backup.delete(key)) {
      deleteCount.incrementAndGet();
      if (destNode instanceof Node.File destFile) {
        bytesRemoved.addAndGet(destFile.size());
      }
    } else {
      failedDeleteCount.incrementAndGet();
    }
  }

  private OverallStatistics getAndLogStats(long runStartNanos) {
    Statistics statistics =
        new Statistics(
            createCount.get(),
            updateCount.get(),
            deleteCount.get(),
            sameCount.get(),
            bytesAdded.get(),
            bytesRemoved.get());
    ErrorStatistics errorStatistics =
        new ErrorStatistics(
            failedCreateCount.get(), failedUpdateCount.get(), failedDeleteCount.get());

    log.info("Finished: {} in: {}. {}", backup, elapsed(runStartNanos), statistics);
    if (errorStatistics.any()) {
      log.warn("{}", errorStatistics);
    }

    return new OverallStatistics(statistics, errorStatistics);
  }

  private static Duration elapsed(long startNanos) {
    return Duration.ofNanos(System.nanoTime() - startNanos);
  }

  /**
   * Statistics over a backup run.
   *
   * @param filesCreated files created on destination
   * @param filesUpdated files updated on destination
   * @param filesDeleted files deleted on destination
   * @param filesSame files kept same
   * @param bytesAdded bytes added to destination
   * @param bytesRemoved bytes removed from destination
   */
  public record Statistics(
      int filesCreated,
      int filesUpdated,
      int filesDeleted,
      int filesSame,
      long bytesAdded,
      long bytesRemoved) {
    public Statistics {
      require(filesCreated >= 0);
      require(filesUpdated >= 0);
      require(filesDeleted >= 0);
      require(filesSame >= 0);
      require(bytesAdded >= 0);
      require(bytesRemoved >= 0);
    }

    @Override
    public String toString() {
      return "%s files created, %s files updated, %s files deleted, %s files same. %sMB added, %sMB removed"
          .formatted(
              NUMBER_FORMAT.format(filesCreated),
              NUMBER_FORMAT.format(filesUpdated),
              NUMBER_FORMAT.format(filesDeleted),
              NUMBER_FORMAT.format(filesSame),
              NUMBER_FORMAT.format(bytesAdded / MEGA),
              NUMBER_FORMAT.format(bytesRemoved / MEGA));
    }
  }

  /**
   * Error statistics over a backup run.
   *
   * @param failedCreates failed creates
   * @param failedUpdates failed updates
   * @param failedDeletes failed deletes
   */
  public record ErrorStatistics(int failedCreates, int failedUpdates, int failedDeletes) {
    public ErrorStatistics {
      require(failedCreates >= 0);
      require(failedUpdates >= 0);
      require(failedDeletes >= 0);
    }

    public boolean any() {
      return IntStream.of(failedCreates, failedUpdates, failedDeletes).anyMatch(i -> i > 0);
    }

    @Override
    public String toString() {
      return "%s failed creates, %s failed updates, %s failed deletes"
          .formatted(
              NUMBER_FORMAT.format(failedCreates),
              NUMBER_FORMAT.format(failedUpdates),
              NUMBER_FORMAT.format(failedDeletes));
    }
  }

  /**
   * Overall statistics over a backup run.
   *
   * @param statistics statistics
   * @param errorStatistics error statistics
   */
  public record OverallStatistics(Statistics statistics, ErrorStatistics errorStatistics) {
    public OverallStatistics {
      requireNonNull(statistics);
      requireNonNull(errorStatistics);
    }
  }
}
