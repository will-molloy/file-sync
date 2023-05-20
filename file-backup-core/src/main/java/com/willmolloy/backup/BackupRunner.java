package com.willmolloy.backup;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
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
  public static boolean run(Backup<?, ?> backup) {
    log.info("Running: {}", backup);
    long runStartNanos = System.nanoTime();

    AtomicBoolean allSuccess = new AtomicBoolean(true);

    try (ExecutorService threadPool =
        Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("worker-", 1).factory())) {

      FileTree sourceFiles = scanWithLog(backup.source()::scan, "source");
      FileTree destFiles = scanWithLog(backup.destination()::scan, "destination");

      sourceFiles.forEach(
          (key, sourceFile) -> {
            if (sourceFiles.descendants(key).anyMatch(descendant -> true)) {
              log.debug("Skipping put({}). Covered by descendant", key);
              return;
            }

            Optional<File> maybeDestFile = destFiles.get(key);
            if (maybeDestFile.isEmpty() || !sourceFile.same(maybeDestFile.get())) {
              threadPool.submit(
                  () -> {
                    log.debug("put({})", key);
                    if (!backup.put(key)) {
                      allSuccess.set(false);
                    }
                  });
            } else {
              log.debug("same({})", key);
            }
          });

      destFiles.forEach(
          (key, destFile) -> {
            if (sourceFiles.contains(key)) {
              return;
            }

            if (destFiles.ancestors(key).anyMatch(ancestor -> !sourceFiles.contains(ancestor))) {
              log.debug("Skipping delete({}). Covered by ancestor", key);
              return;
            }

            threadPool.submit(
                () -> {
                  log.debug("delete({})", key);
                  if (!backup.delete(key)) {
                    allSuccess.set(false);
                  }
                });
          });
    }

    log.info("Finished: {} in: {}", backup, elapsed(runStartNanos));
    if (isRunningInsideDocker()) {
      try {
        log.info("Sleep 1 hour to view logs");
        Thread.sleep(Duration.ofHours(1));
      } catch (InterruptedException e) {
        log.error("Interrupted", e);
        Thread.currentThread().interrupt();
      }
    }
    return allSuccess.get();
  }

  @SuppressFBWarnings("DMI_HARDCODED_ABSOLUTE_FILENAME")
  private static boolean isRunningInsideDocker() {
    return new java.io.File("/.dockerenv").exists();
  }

  private static FileTree scanWithLog(Supplier<FileTree> scan, String locationForLog) {
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

  private static Duration elapsed(long startNanos) {
    return Duration.ofNanos(System.nanoTime() - startNanos);
  }

  private BackupRunner() {}
}
