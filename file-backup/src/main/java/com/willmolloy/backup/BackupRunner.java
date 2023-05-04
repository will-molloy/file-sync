package com.willmolloy.backup;

import static com.willmolloy.backup.util.Preconditions.require;
import static java.util.Objects.requireNonNull;

import com.willmolloy.backup.Backup.File;
import com.willmolloy.backup.Backup.Location;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Runs a {@link Backup}.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
class BackupRunner {

  private static final Logger log = LogManager.getLogger();

  private final Backup<?, ?> backup;
  private final Location source;
  private final Location destination;

  BackupRunner(Backup<?, ?> backup) {
    this.backup = requireNonNull(backup);
    this.source = backup.source();
    this.destination = backup.destination();
  }

  OverallStatistics run() {
    log.info("Running: {}", backup);
    long startNanos = System.nanoTime();

    AtomicLong copyCount = new AtomicLong();
    AtomicLong failedCopyCount = new AtomicLong();
    AtomicLong updateCount = new AtomicLong();
    AtomicLong failedUpdateCount = new AtomicLong();
    AtomicLong deleteCount = new AtomicLong();
    AtomicLong failedDeleteCount = new AtomicLong();

    try (ExecutorService executorService =
        Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("worker-", 1).factory())) {

      long sourceScanStart = System.nanoTime();
      Map<String, File> sourceFiles = source.scan();
      log.info("Scanned {} files in source, in: {}", sourceFiles.size(), elapsed(sourceScanStart));

      long destScanStart = System.nanoTime();
      Map<String, File> destFiles = destination.scan();
      log.info("Scanned {} files in destination, in: {}", destFiles.size(), elapsed(destScanStart));

      Stream<Runnable> copiesAndUpdates =
          sourceFiles.entrySet().stream()
              .map(
                  e ->
                      () -> {
                        String key = e.getKey();

                        File sourceFile = e.getValue();
                        File destFile = destFiles.get(key);

                        if (destFile == null) {
                          if (backup.put(key)) {
                            copyCount.incrementAndGet();
                          } else {
                            failedCopyCount.incrementAndGet();
                          }
                        } else if (!sourceFile.size().equals(destFile.size())
                            || !sourceFile.lastModified().equals(destFile.lastModified())) {
                          if (backup.put(key)) {
                            updateCount.incrementAndGet();
                          } else {
                            failedUpdateCount.incrementAndGet();
                          }
                        }
                      });

      Stream<Runnable> deletes =
          destFiles.keySet().stream()
              .map(
                  key ->
                      () -> {
                        if (!sourceFiles.containsKey(key)) {
                          if (backup.delete(key)) {
                            deleteCount.incrementAndGet();
                          } else {
                            failedDeleteCount.incrementAndGet();
                          }
                        }
                      });

      Stream.concat(copiesAndUpdates, deletes).forEach(executorService::submit);
    }

    Statistics statistics = new Statistics(copyCount.get(), updateCount.get(), deleteCount.get());
    ErrorStatistics errorStatistics =
        new ErrorStatistics(
            failedCopyCount.get(), failedUpdateCount.get(), failedDeleteCount.get());

    if (!errorStatistics.any()) {
      log.info("Finished: {}, with {}, in: {}", backup, statistics, elapsed(startNanos));
    } else {
      log.error(
          "Finished: {}, with {} and {}, in: {}",
          backup,
          statistics,
          errorStatistics,
          elapsed(startNanos));
    }

    return new OverallStatistics(statistics, errorStatistics);
  }

  private Duration elapsed(long startNanos) {
    return Duration.ofNanos(System.nanoTime() - startNanos);
  }

  /**
   * Statistics over a backup run.
   *
   * @param copies copy count
   * @param updates update count
   * @param deletes delete count
   */
  record Statistics(long copies, long updates, long deletes) {
    Statistics {
      require(copies >= 0);
      require(updates >= 0);
      require(deletes >= 0);
    }
  }

  /**
   * Error statistics over a backup run.
   *
   * @param failedCopies failed copy count
   * @param failedUpdates failed update count
   * @param failedDeletes failed delete count
   */
  record ErrorStatistics(long failedCopies, long failedUpdates, long failedDeletes) {
    ErrorStatistics {
      require(failedCopies >= 0);
      require(failedUpdates >= 0);
      require(failedDeletes >= 0);
    }

    boolean any() {
      return failedCopies > 0 || failedUpdates > 0 || failedDeletes > 0;
    }
  }

  /**
   * Overall statistics over a backup run.
   *
   * @param statistics statistics
   * @param errorStatistics error statistics
   */
  record OverallStatistics(Statistics statistics, ErrorStatistics errorStatistics) {
    OverallStatistics {
      requireNonNull(statistics);
      requireNonNull(errorStatistics);
    }
  }
}
