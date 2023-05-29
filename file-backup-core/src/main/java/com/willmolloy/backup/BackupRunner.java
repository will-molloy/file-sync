package com.willmolloy.backup;

import static com.willmolloy.backup.util.TimeHelper.elapsed;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
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

  /** Runs the backup. */
  public static <SourceFileT extends File, DestFileT extends File> boolean run(
      Backup<SourceFileT, DestFileT> backup) {
    log.info("Running: {}", backup);
    long runStartNanos = System.nanoTime();

    AtomicBoolean allSuccess = new AtomicBoolean(true);

    FileTree<SourceFileT> sourceFileTree = backup.source().fileTree();
    FileTree<DestFileT> destFileTree = backup.destination().fileTree();

    try (ExecutorService threadPool =
        Executors.newFixedThreadPool(10, Thread.ofVirtual().name("worker-", 1).factory())) {

      Predicate<DestFileT> canDelete =
          destFile -> {
            Optional<SourceFileT> maybeSourceFile = sourceFileTree.get(destFile.relativePath());
            return maybeSourceFile.isEmpty() || !maybeSourceFile.get().same(destFile);
          };
      destFileTree
          .postorder()
          .filter(canDelete)
          // skip delete if covered by ancestor
          .filter(destFile -> destFileTree.ancestors(destFile).noneMatch(canDelete))
          .forEach(
              destFile ->
                  threadPool.submit(
                      () -> {
                        if (!backup.delete(destFile)) {
                          allSuccess.set(false);
                        }
                      }));
    }

    try (ExecutorService threadPool =
        Executors.newFixedThreadPool(10, Thread.ofVirtual().name("worker-", 1).factory())) {

      Predicate<SourceFileT> canPut =
          sourceFile -> {
            Optional<DestFileT> maybeDestFile = destFileTree.get(sourceFile.relativePath());
            return maybeDestFile.isEmpty() || !sourceFile.same(maybeDestFile.get());
          };
      sourceFileTree
          // only need to put leaves
          .leaves()
          .filter(canPut)
          .forEach(
              sourceFile ->
                  threadPool.submit(
                      () -> {
                        log.debug("put({})", sourceFile);
                        if (!backup.put(sourceFile)) {
                          allSuccess.set(false);
                        }
                      }));
    }

    log.info("Finished: {} in: {}", backup, elapsed(runStartNanos));
    return allSuccess.get();
  }

  private BackupRunner() {}
}
