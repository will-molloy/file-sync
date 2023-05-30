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

    // Doing delete first; otherwise there are scenarios where put can fail, e.g. non-empty dir
    // overwriting a file
    log.info("Deleting files on destination that aren't on source");
    try (ExecutorService threadPool = threadPool("delete")) {
      // TODO push down into Backup class - for S3, delete before update is a waste
      Predicate<DestFileT> canDelete =
          destFile -> {
            // don't delete the root, it was created manually outside this app; if it's deleted
            // subsequent runs will fail
            if (destFileTree.isRoot(destFile)) {
              return false;
            }
            Optional<SourceFileT> maybeSourceFile = sourceFileTree.get(destFile.relativePath());
            // either file not on source -> delete
            // OR files different -> will update
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
                        // TODO make the logs here info and the ones below debug?
                        log.debug("delete({})", destFile);
                        if (!backup.delete(destFile)) {
                          allSuccess.set(false);
                        }
                      }));
    }

    log.info("Copying files from source that aren't on destination");
    try (ExecutorService threadPool = threadPool("put")) {
      Predicate<SourceFileT> canPut =
          sourceFile -> {
            if (sourceFileTree.isRoot(sourceFile)) {
              return false;
            }
            Optional<DestFileT> maybeDestFile = destFileTree.get(sourceFile.relativePath());
            // either file not on dest -> create
            // OR files different -> update
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

  private static ExecutorService threadPool(String name) {
    return Executors.newThreadPerTaskExecutor(
        Thread.ofVirtual().name("%s-worker-".formatted(name), 1).factory());
  }

  private BackupRunner() {}
}
