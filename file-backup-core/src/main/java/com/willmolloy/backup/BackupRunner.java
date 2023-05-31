package com.willmolloy.backup;

import static com.willmolloy.backup.util.TimeHelper.elapsed;
import static java.util.Objects.requireNonNull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Contains the core algorithm for running a {@link Backup}.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
@SuppressFBWarnings(
    value = "RV_RETURN_VALUE_IGNORED_BAD_PRACTICE",
    justification = "Relying on default ExecutorService.close to wait for futures")
final class BackupRunner<SourceFileT extends File, DestFileT extends File> {
  private static final Logger log = LogManager.getLogger();

  // highly coupled to Backup; could've been abstract class but prefer composition for testability
  private final Backup<SourceFileT, DestFileT> backup;

  BackupRunner(Backup<SourceFileT, DestFileT> backup) {
    this.backup = requireNonNull(backup);
  }

  boolean run() {
    log.info("Running: {}", backup);
    long runStartNanos = System.nanoTime();

    AtomicBoolean allSuccess = new AtomicBoolean(true);

    FileTree<SourceFileT> sourceFileTree = backup.source().fileTree();
    FileTree<DestFileT> destFileTree = backup.destination().fileTree();

    // Doing deletes first; otherwise there are scenarios where put can fail, e.g. non-empty dir
    // overwriting a file
    try (ExecutorService threadPool = threadPool("delete")) {
      destFileTree
          .postorder()
          .filter(backup::needDelete)
          // skip delete if covered by ancestor, since children are deleted too
          .filter(destFile -> destFileTree.ancestors(destFile).noneMatch(backup::needDelete))
          .forEach(
              destFile ->
                  threadPool.submit(
                      () -> {
                        // TODO make the logs here info and the ones below debug?
                        log.debug("delete({})", destFile);
                        if (!backup.delete(destFile)) {
                          allSuccess.set(false);
                        }
                      }));
    }

    try (ExecutorService threadPool = threadPool("put")) {
      sourceFileTree
          // only need to put leaves, parents are created as necessary
          .leaves()
          .filter(backup::needPut)
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

  private static ExecutorService threadPool(String name) {
    return Executors.newThreadPerTaskExecutor(
        Thread.ofVirtual().name("%s-worker-".formatted(name), 1).factory());
  }
}
