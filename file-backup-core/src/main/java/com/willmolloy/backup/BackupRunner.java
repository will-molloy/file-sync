package com.willmolloy.backup;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
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

      FileTree<SourceFileT> sourceFileTree = scanWithLog(backup.source()::scan, "source");
      FileTree<DestFileT> destFileTree = scanWithLog(backup.destination()::scan, "destination");

      sourceFileTree
          .preorder()
          // only need to put leaves
          .filter(
              sourceFile -> sourceFileTree.descendants(sourceFile).noneMatch(descendant -> true))
          .filter(
              sourceFile -> {
                Optional<DestFileT> maybeDestFile = destFileTree.get(sourceFile.relativePath());
                if (maybeDestFile.isEmpty() || !sourceFile.same(maybeDestFile.get())) {
                  return true;
                }
                log.debug("Skipping put. Files same({}, {})", sourceFile, maybeDestFile.get());
                return false;
              })
          .forEach(
              sourceFile ->
                  threadPool.submit(
                      () -> {
                        EntryMessage m = log.traceEntry("put({})", sourceFile);
                        if (!log.traceExit(m, backup.put(sourceFile))) {
                          allSuccess.set(false);
                        }
                      }));

      Predicate<DestFileT> canDelete =
          destFile -> sourceFileTree.get(destFile.relativePath()).isEmpty();
      destFileTree
          .preorder()
          .filter(canDelete)
          // skip delete if covered by ancestor
          .filter(destFile -> destFileTree.ancestors(destFile).noneMatch(canDelete))
          .forEach(
              destFile ->
                  threadPool.submit(
                      () -> {
                        EntryMessage m = log.traceEntry("delete({})", destFile);
                        if (!log.traceExit(m, backup.delete(destFile))) {
                          allSuccess.set(false);
                        }
                      }));
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
