package com.willmolloy.backup;

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

  Statistics run() {
    log.info("Running: {}", backup);
    long startNanos = System.nanoTime();

    AtomicLong copyCount = new AtomicLong();
    AtomicLong updateCount = new AtomicLong();
    AtomicLong deleteCount = new AtomicLong();

    try (ExecutorService executorService =
        Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("worker-", 1).factory())) {

      long sourceScanStart = System.nanoTime();
      Map<String, File> sourceFiles = source.scan();
      log.info("Scanned source in: {}", elapsed(sourceScanStart));

      long destScanStart = System.nanoTime();
      Map<String, File> destFiles = destination.scan();
      log.info("Scanned destination in: {}", elapsed(destScanStart));

      Stream<Runnable> copiesAndUpdates =
          sourceFiles.entrySet().stream()
              .map(
                  e ->
                      () -> {
                        String key = e.getKey();

                        File sourceFile = e.getValue();
                        File destFile = destFiles.get(key);

                        if (destFile == null) {
                          copyCount.incrementAndGet();
                          backup.put(key);
                        } else if (!sourceFile.size().equals(destFile.size())
                            || !sourceFile.lastModified().equals(destFile.lastModified())) {
                          updateCount.incrementAndGet();
                          backup.put(key);
                        }
                      });

      Stream<Runnable> deletes =
          destFiles.keySet().stream()
              .map(
                  key ->
                      () -> {
                        if (!sourceFiles.containsKey(key)) {
                          deleteCount.incrementAndGet();
                          backup.delete(key);
                        }
                      });

      Stream.concat(copiesAndUpdates, deletes).forEach(executorService::submit);
    }

    Statistics statistics = new Statistics(copyCount.get(), updateCount.get(), deleteCount.get());
    log.info("Finished: {}, with {}, in: {}", backup, statistics, elapsed(startNanos));
    return statistics;
  }

  private Duration elapsed(long startNanos) {
    return Duration.ofNanos(System.nanoTime() - startNanos);
  }

  /**
   * Backup statistics.
   *
   * @param copies number of copies
   * @param updates number of updates
   * @param deletes number of deletes
   */
  record Statistics(long copies, long updates, long deletes) {}
}
