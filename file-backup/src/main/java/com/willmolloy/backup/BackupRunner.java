package com.willmolloy.backup;

import static com.willmolloy.backup.util.Preconditions.require;
import static java.util.Objects.requireNonNull;

import com.willmolloy.backup.Backup.File;
import com.willmolloy.backup.Backup.Location;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
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

    AtomicInteger createCount = new AtomicInteger();
    AtomicInteger failedCreateCount = new AtomicInteger();
    AtomicInteger updateCount = new AtomicInteger();
    AtomicInteger failedUpdateCount = new AtomicInteger();
    AtomicInteger deleteCount = new AtomicInteger();
    AtomicInteger failedDeleteCount = new AtomicInteger();
    AtomicInteger sameCount = new AtomicInteger();

    try (ExecutorService executorService =
        Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("worker-", 1).factory())) {

      long sourceScanStart = System.nanoTime();
      Map<String, File> sourceFiles = source.scan();
      log.info("Scanned source with {} files in: {}", sourceFiles.size(), elapsed(sourceScanStart));

      long destScanStart = System.nanoTime();
      Map<String, File> destFiles = destination.scan();
      log.info(
          "Scanned destination with {} files in: {}", destFiles.size(), elapsed(destScanStart));

      Stream<Runnable> createsAndUpdates =
          sourceFiles.entrySet().stream()
              .map(
                  e ->
                      () -> {
                        String key = e.getKey();
                        File sourceFile = e.getValue();
                        File destFile = destFiles.get(key);

                        if (destFile == null) {
                          if (backup.put(key)) {
                            createCount.incrementAndGet();
                          } else {
                            failedCreateCount.incrementAndGet();
                          }
                        } else if (!sourceFile.etag().equals(destFile.etag())) {
                          if (backup.put(key)) {
                            updateCount.incrementAndGet();
                          } else {
                            failedUpdateCount.incrementAndGet();
                          }
                        } else {
                          sameCount.incrementAndGet();
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

      Stream.concat(createsAndUpdates, deletes).forEach(executorService::submit);
    }

    Statistics statistics =
        new Statistics(createCount.get(), updateCount.get(), deleteCount.get(), sameCount.get());
    ErrorStatistics errorStatistics =
        new ErrorStatistics(
            failedCreateCount.get(), failedUpdateCount.get(), failedDeleteCount.get());

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
   * @param filesCreated files created
   * @param filesUpdated files updated
   * @param filesDeleted files deleted
   * @param filesSame files kept same
   */
  record Statistics(int filesCreated, int filesUpdated, int filesDeleted, int filesSame) {
    Statistics {
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
  record ErrorStatistics(int failedCreates, int failedUpdates, int failedDeletes) {
    ErrorStatistics {
      require(failedCreates >= 0);
      require(failedUpdates >= 0);
      require(failedDeletes >= 0);
    }

    boolean any() {
      return IntStream.of(failedCreates, failedUpdates, failedDeletes).anyMatch(i -> i > 0);
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
