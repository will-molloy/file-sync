package com.willmolloy.backup;

import static com.willmolloy.backup.util.Preconditions.require;
import static java.util.Objects.requireNonNull;

import com.willmolloy.backup.Backup.File;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
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

  private static final int MEGA = 1_000_000;

  /** Runs the backup. */
  // static factory ensures single instance per run
  public static OverallStatistics run(Backup<?, ?> backup) {
    return new BackupRunner(backup).run();
  }

  private final NumberFormat numberFormat = NumberFormat.getInstance(Locale.ENGLISH);
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
    long startNanos = System.nanoTime();

    try (ExecutorService threadPool =
        Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("worker-", 1).factory())) {

      Map<String, ? extends File> sourceFiles = scanWithLog(backup.source()::scan, "source");
      Map<String, ? extends File> destFiles =
          scanWithLog(backup.destination()::scan, "destination");

      sourceFiles.forEach(
          (key, sourceFile) -> {
            File destFile = destFiles.get(key);
            if (destFile == null) {
              threadPool.submit(() -> create(key, sourceFile));
            } else if (!sourceFile.same(destFile)) {
              threadPool.submit(() -> update(key, sourceFile, destFile));
            } else {
              sameCount.incrementAndGet();
            }
          });

      destFiles.forEach(
          (key, destFile) -> {
            if (!sourceFiles.containsKey(key)) {
              threadPool.submit(() -> delete(key, destFile));
            }
          });
    }

    Statistics statistics =
        new Statistics(createCount.get(), updateCount.get(), deleteCount.get(), sameCount.get());
    ErrorStatistics errorStatistics =
        new ErrorStatistics(
            failedCreateCount.get(), failedUpdateCount.get(), failedDeleteCount.get());

    long addedMB = bytesAdded.get() / MEGA;
    long removedMB = bytesRemoved.get() / MEGA;

    log.info(
        "Finished: {} in: {}. {} files created, {} files updated, {} files deleted, {} files same. {}MB added, {}MB removed",
        backup,
        elapsed(startNanos),
        numberFormat.format(statistics.filesCreated()),
        numberFormat.format(statistics.filesUpdated()),
        numberFormat.format(statistics.filesDeleted()),
        numberFormat.format(statistics.filesSame()),
        numberFormat.format(addedMB),
        numberFormat.format(removedMB));
    if (errorStatistics.any()) {
      log.warn(
          "{} failed creates, {} failed updates, {} failed deletes",
          numberFormat.format(errorStatistics.failedCreates()),
          numberFormat.format(errorStatistics.failedUpdates()),
          numberFormat.format(errorStatistics.failedDeletes()));
    }

    return new OverallStatistics(statistics, errorStatistics);
  }

  private Map<String, ? extends File> scanWithLog(
      Supplier<Map<String, ? extends File>> scan, String locationForLog) {
    long scanStart = System.nanoTime();
    Map<String, ? extends File> files = scan.get();
    long sizeMB = files.values().stream().mapToLong(File::size).sum() / MEGA;
    log.info(
        "Scanned {} in: {}. {} files. {}MB",
        locationForLog,
        elapsed(scanStart),
        numberFormat.format(files.size()),
        numberFormat.format(sizeMB));
    return files;
  }

  private void create(String key, File sourceFile) {
    if (backup.put(key)) {
      createCount.incrementAndGet();
      bytesAdded.addAndGet(sourceFile.size());
    } else {
      failedCreateCount.incrementAndGet();
    }
  }

  private void update(String key, File sourceFile, File destFile) {
    if (backup.put(key)) {
      updateCount.incrementAndGet();
      bytesAdded.addAndGet(sourceFile.size());
      bytesRemoved.addAndGet(destFile.size());
    } else {
      failedUpdateCount.incrementAndGet();
    }
  }

  private void delete(String key, File destFile) {
    if (backup.delete(key)) {
      deleteCount.incrementAndGet();
      bytesRemoved.addAndGet(destFile.size());
    } else {
      failedDeleteCount.incrementAndGet();
    }
  }

  private static Duration elapsed(long startNanos) {
    return Duration.ofNanos(System.nanoTime() - startNanos);
  }

  /**
   * Statistics over a backup run.
   *
   * @param filesCreated files created
   * @param filesUpdated files updated
   * @param filesDeleted files deleted
   * @param filesSame files kept same
   */
  public record Statistics(int filesCreated, int filesUpdated, int filesDeleted, int filesSame) {
    public Statistics {
      require(filesCreated >= 0);
      require(filesUpdated >= 0);
      require(filesDeleted >= 0);
      require(filesSame >= 0);
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
