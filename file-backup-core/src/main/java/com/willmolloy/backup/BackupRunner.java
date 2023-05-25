package com.willmolloy.backup;

import static java.util.function.Predicate.not;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.file.Path;
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
import org.apache.logging.log4j.message.EntryMessage;

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
  public static <SourceFileT extends File, DestFileT extends File> boolean run(
      Backup<SourceFileT, DestFileT> backup) {
    log.info("Running: {}", backup);
    long runStartNanos = System.nanoTime();

    AtomicBoolean allSuccess = new AtomicBoolean(true);

    try (ExecutorService threadPool =
        Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("worker-", 1).factory())) {

      FileTree<SourceFileT> sourceFiles = scanWithLog(backup.source()::scan, "source");
      FileTree<DestFileT> destFiles = scanWithLog(backup.destination()::scan, "destination");

      sourceFiles.forEach(
          (sourceFile) -> {
            Path key = sourceFile.relativePath();

            if (sourceFiles.descendants(key).anyMatch(descendant -> true)) {
              log.debug("Skipping put({}). Covered by descendant", key);
              return;
            }

            Optional<DestFileT> maybeDestFile = destFiles.get(key);
            if (maybeDestFile.isEmpty() || !sourceFile.same(maybeDestFile.get())) {
              threadPool.submit(
                  () -> {
                    EntryMessage m = log.traceEntry("put({})", sourceFile);
                    if (!log.traceExit(m, backup.put(sourceFile))) {
                      allSuccess.set(false);
                    }
                  });
            } else {
              log.trace("same({})", key);
            }
          });

      destFiles.forEach(
          (destFile) -> {
            Path key = destFile.relativePath();

            if (sourceFiles.contains(key)) {
              return;
            }

            if (destFiles
                .ancestors(key)
                .map(File::relativePath)
                .anyMatch(not(sourceFiles::contains))) {
              log.debug("Skipping delete({}). Covered by ancestor", key);
              return;
            }

            threadPool.submit(
                () -> {
                  EntryMessage m = log.traceEntry("delete({})", destFile);
                  if (!log.traceExit(m, backup.delete(destFile))) {
                    allSuccess.set(false);
                  }
                });
          });
    }

    log.info("Finished: {} in: {}", backup, elapsed(runStartNanos));
    return allSuccess.get();
  }

  private static <FileT extends File> FileTree<FileT> scanWithLog(
      Supplier<FileTree<FileT>> scan, String locationForLog) {
    long scanStartNanos = System.nanoTime();
    FileTree<FileT> fileTree = scan.get();
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
