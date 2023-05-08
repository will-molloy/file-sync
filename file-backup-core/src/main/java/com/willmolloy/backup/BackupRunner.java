package com.willmolloy.backup;

import static com.willmolloy.backup.util.Preconditions.require;
import static java.util.Objects.requireNonNull;

import com.willmolloy.backup.Backup.File;
import com.willmolloy.backup.Backup.Location;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Runs a {@link Backup}.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public class BackupRunner {

  private static final Logger log = LogManager.getLogger();

  private static final int MEGA = 1_000_000;

  private final NumberFormat numberFormat = NumberFormat.getInstance(Locale.ENGLISH);

  private final Backup<?, ?> backup;
  private final Location source;
  private final Location destination;

  public BackupRunner(Backup<?, ?> backup) {
    this.backup = requireNonNull(backup);
    this.source = backup.source();
    this.destination = backup.destination();
  }

  /** Runs the backup. */
  public OverallStatistics run() {
    log.info("Running: {}", backup);
    long startNanos = System.nanoTime();

    AtomicInteger createCount = new AtomicInteger();
    AtomicInteger failedCreateCount = new AtomicInteger();
    AtomicInteger updateCount = new AtomicInteger();
    AtomicInteger failedUpdateCount = new AtomicInteger();
    AtomicInteger deleteCount = new AtomicInteger();
    AtomicInteger failedDeleteCount = new AtomicInteger();
    AtomicInteger sameCount = new AtomicInteger();

    // long can represent up to 9.2 EB
    AtomicLong bytesAdded = new AtomicLong();
    AtomicLong bytesRemoved = new AtomicLong();

    try (ExecutorService threadPool =
        Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("worker-", 1).factory())) {

      long sourceScanStart = System.nanoTime();
      Map<String, File> sourceFiles = source.scan();
      long sourceSizeMB = sourceFiles.values().stream().mapToLong(File::size).sum() / MEGA;
      log.info(
          "Scanned source in: {}. {} files. {}MB",
          elapsed(sourceScanStart),
          numberFormat.format(sourceFiles.size()),
          numberFormat.format(sourceSizeMB));

      long destScanStart = System.nanoTime();
      Map<String, File> destFiles = destination.scan();
      long destSizeMB = destFiles.values().stream().mapToLong(File::size).sum() / MEGA;
      log.info(
          "Scanned destination in: {}. {} files. {}MB",
          elapsed(destScanStart),
          numberFormat.format(destFiles.size()),
          numberFormat.format(destSizeMB));

      Stream<Runnable> createsAndUpdates =
          sourceFiles.entrySet().stream()
              .map(
                  e ->
                      () -> {
                        String key = e.getKey();
                        File sourceFile = e.getValue();
                        File destFile = destFiles.get(key);
                        long sourceFileSize = sourceFile.size();

                        if (destFile == null) {
                          if (backup.put(key)) {
                            createCount.incrementAndGet();
                            bytesAdded.addAndGet(sourceFileSize);
                          } else {
                            failedCreateCount.incrementAndGet();
                          }

                        } else if (!sourceFile.sameContents(destFile)) {
                          long destFileSize = destFile.size();

                          if (backup.put(key)) {
                            updateCount.incrementAndGet();
                            bytesAdded.addAndGet(sourceFileSize);
                            bytesRemoved.addAndGet(destFileSize);
                          } else {
                            failedUpdateCount.incrementAndGet();
                          }

                        } else {
                          sameCount.incrementAndGet();
                        }
                      });

      Stream<Runnable> deletes =
          destFiles.entrySet().stream()
              .map(
                  e ->
                      () -> {
                        String key = e.getKey();
                        File destFile = e.getValue();
                        long destFileSize = destFile.size();

                        if (!sourceFiles.containsKey(key)) {
                          if (backup.delete(key)) {
                            deleteCount.incrementAndGet();
                            bytesRemoved.addAndGet(destFileSize);
                          } else {
                            failedDeleteCount.incrementAndGet();
                          }
                        }
                      });

      Stream.concat(createsAndUpdates, deletes).forEach(threadPool::submit);
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

  private Duration elapsed(long startNanos) {
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
