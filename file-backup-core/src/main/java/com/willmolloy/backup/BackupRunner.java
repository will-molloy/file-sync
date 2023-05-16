package com.willmolloy.backup;

import static com.willmolloy.backup.util.Preconditions.require;
import static java.util.Objects.requireNonNull;

import com.willmolloy.backup.FileTree.File;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.LongStream;
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

  private final AtomicLong createCount = new AtomicLong();
  private final AtomicLong failedCreateCount = new AtomicLong();
  private final AtomicLong updateCount = new AtomicLong();
  private final AtomicLong failedUpdateCount = new AtomicLong();
  private final AtomicLong deleteCount = new AtomicLong();
  private final AtomicLong failedDeleteCount = new AtomicLong();
  private final AtomicLong sameCount = new AtomicLong();
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
          (key, sourceFile) -> {
            if (sourceFiles.containsAnyChildOf(key)) {
              log.debug("Skipping put({}). Covered by child", key);
              return;
            }

            File destFile = destFiles.get(key);
            if (destFile == null) {
              log.debug("create({})", key);
              threadPool.submit(() -> create(key, sourceFile));
            } else if (!sourceFile.same(destFile)) {
              log.debug("update({})", key);
              threadPool.submit(() -> update(key, sourceFile, destFile));
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

            if (destFiles.containsParentOf(key) && !sourceFiles.containsParentOf(key)) {
              log.debug("Skipping delete({}). Covered by parent", key);
              return;
            }

            log.debug("delete({})", key);
            threadPool.submit(() -> delete(key, destFiles));
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
        NUMBER_FORMAT.format(fileTree.leafCount()),
        NUMBER_FORMAT.format(fileTree.totalSize() / MEGA));

    return fileTree;
  }

  private void create(Path key, File sourceFile) {
    if (backup.put(key)) {
      createCount.incrementAndGet();
      bytesAdded.addAndGet(sourceFile.size());
    } else {
      failedCreateCount.incrementAndGet();
    }
  }

  private void update(Path key, File sourceFile, File destFile) {
    if (backup.put(key)) {
      updateCount.incrementAndGet();
      bytesAdded.addAndGet(sourceFile.size());
      bytesRemoved.addAndGet(destFile.size());
    } else {
      failedUpdateCount.incrementAndGet();
    }
  }

  private void delete(Path key, FileTree destFiles) {
    if (backup.delete(key)) {
      FileTree childTree = destFiles.childTree(key);
      deleteCount.addAndGet(childTree.leafCount());
      bytesRemoved.addAndGet(childTree.totalSize());
    } else {
      FileTree childTree = destFiles.childTree(key);
      failedDeleteCount.addAndGet(childTree.leafCount());
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
      long filesCreated,
      long filesUpdated,
      long filesDeleted,
      long filesSame,
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
  public record ErrorStatistics(long failedCreates, long failedUpdates, long failedDeletes) {
    public ErrorStatistics {
      require(failedCreates >= 0);
      require(failedUpdates >= 0);
      require(failedDeletes >= 0);
    }

    public boolean any() {
      return LongStream.of(failedCreates, failedUpdates, failedDeletes).anyMatch(i -> i > 0);
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
