package com.willmolloy.backup;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Stopwatch;
import com.google.errorprone.annotations.ForOverride;
import com.willmolloy.backup.statistics.BackupObserver;
import com.willmolloy.backup.statistics.Statistics;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Contains the base algorithm for running a backup.
 *
 * @param <SourceFileT> source file type
 * @param <DestFileT> destination file type
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
@SuppressFBWarnings(
    value = "RV_RETURN_VALUE_IGNORED_BAD_PRACTICE",
    justification = "Relying on default ExecutorService.close to wait for futures")
public abstract class BaseBackup<SourceFileT extends File, DestFileT extends File>
    implements Backup<SourceFileT, DestFileT> {
  private static final Logger log = LogManager.getLogger();

  private static ExecutorService threadPool(String name) {
    return Executors.newThreadPerTaskExecutor(
        Thread.ofVirtual().name("%s-worker-".formatted(name), 1).factory());
  }

  private final Location<SourceFileT> source;
  private final Location<DestFileT> destination;
  private final List<BackupObserver> observers;

  protected BaseBackup(
      Location<SourceFileT> source,
      Location<DestFileT> destination,
      List<BackupObserver> observers) {
    this.source = checkNotNull(source);
    this.destination = checkNotNull(destination);
    this.observers = List.copyOf(observers);
  }

  @Override
  public final Location<SourceFileT> source() {
    return source;
  }

  @Override
  public final Location<DestFileT> destination() {
    return destination;
  }

  /** Runs the backup. */
  public final boolean run() {
    try {
      Stopwatch stopwatch = Stopwatch.createStarted();
      Statistics<SourceFileT, DestFileT> statistics = new Statistics<>();
      for (BackupObserver observer : observers) {
        observer.notifyStarted(this);
      }

      FileTree<SourceFileT> sourceFileTree = scan(source);
      FileTree<DestFileT> destFileTree = scan(destination);

      // Doing deletes first; otherwise there are scenarios where put can fail, e.g. non-empty dir
      // overwriting a file
      executeDeletes(sourceFileTree, destFileTree, statistics);
      executePuts(sourceFileTree, destFileTree, statistics);

      Statistics.Snapshot snapshot = statistics.snapshot();
      Duration elapsed = stopwatch.elapsed();
      for (BackupObserver observer : observers) {
        observer.notifyFinished(this, snapshot, elapsed);
      }
      return !snapshot.anyErrors();
    } catch (Throwable t) {
      for (BackupObserver observer : observers) {
        observer.notifyFailed(this, t);
      }
      return false;
    }
  }

  private <T extends File> FileTree<T> scan(Location<T> location) {
    Stopwatch stopwatch = Stopwatch.createStarted();
    log.info("Scanning: {}", location);
    FileTree<T> fileTree = location.scan();
    Duration elapsed = stopwatch.elapsed();
    for (BackupObserver observer : observers) {
      observer.notifyScanned(location, fileTree, elapsed);
    }
    return fileTree;
  }

  private void executeDeletes(
      FileTree<SourceFileT> sourceFileTree,
      FileTree<DestFileT> destFileTree,
      Statistics<SourceFileT, DestFileT> statistics) {
    try (ExecutorService threadPool = threadPool("delete")) {
      destFileTree
          .postorder()
          .filter(skipRoot(destFileTree))
          .filter(destFile -> needDelete(sourceFileTree.correspondent(destFile), destFile))
          // skip delete if covered by ancestor, since children are deleted too
          .filter(
              destFile ->
                  destFileTree
                      .ancestors(destFile)
                      .filter(skipRoot(destFileTree))
                      .noneMatch(
                          ancestor -> needDelete(sourceFileTree.correspondent(ancestor), ancestor)))
          .forEach(
              destFile ->
                  threadPool.submit(
                      () -> {
                        log.info("delete({})", destFile);
                        FileTree<DestFileT> subtree = destFileTree.subtree(destFile);
                        if (delete(subtree)) {
                          statistics.countDelete(subtree);
                        } else {
                          statistics.countFailedDelete(subtree);
                        }
                      }));
    }
  }

  private void executePuts(
      FileTree<SourceFileT> sourceFileTree,
      FileTree<DestFileT> destFileTree,
      Statistics<SourceFileT, DestFileT> statistics) {
    try (ExecutorService threadPool = threadPool("put")) {
      sourceFileTree
          // only need to put leaves, parents are created as necessary
          .leaves()
          .filter(skipRoot(sourceFileTree))
          .forEach(
              sourceFile -> {
                Optional<DestFileT> optionalDestFile = destFileTree.correspondent(sourceFile);
                if (needCreate(sourceFile, optionalDestFile)
                    // if it was deleted first, count as create rather than update
                    || needDelete(Optional.of(sourceFile), optionalDestFile.orElseThrow())) {
                  threadPool.submit(
                      () -> {
                        log.info("create({})", sourceFile);
                        if (put(sourceFile)) {
                          statistics.countCreate(sourceFile);
                        } else {
                          statistics.countFailedCreate(sourceFile);
                        }
                      });
                } else {
                  DestFileT destFile = optionalDestFile.orElseThrow();
                  if (needUpdate(sourceFile, destFile)) {
                    threadPool.submit(
                        () -> {
                          log.info("update({}, {})", sourceFile, destFile);
                          if (put(sourceFile)) {
                            statistics.countUpdate(sourceFile, optionalDestFile.get());
                          } else {
                            statistics.countFailedUpdate(sourceFile, optionalDestFile.get());
                          }
                        });
                  } else {
                    log.debug("same({}, {})", sourceFile, destFile);
                    statistics.countSame();
                  }
                }
              });
    }
  }

  private <T extends File> Predicate<T> skipRoot(FileTree<T> fileTree) {
    return file -> file != fileTree.root();
  }

  /**
   * Creates or updates the corresponding file on destination.
   *
   * @return {@code true} if create/update was successful
   * @implSpec Creates parent directories as necessary
   */
  @ForOverride
  protected abstract boolean put(SourceFileT sourceFile);

  /**
   * Deletes the subtree on destination.
   *
   * @return {@code true} if delete was successful
   * @implSpec Deletes all child directories/files
   */
  @ForOverride
  protected abstract boolean delete(FileTree<DestFileT> destSubtree);

  /** {@code true} if create (via {@link #put}) is necessary. */
  @ForOverride
  protected boolean needCreate(SourceFileT sourceFile, Optional<DestFileT> optionalDestFile) {
    return optionalDestFile.isEmpty();
  }

  /** {@code true} if update (via {@link #put}) is necessary. */
  @ForOverride
  protected boolean needUpdate(SourceFileT sourceFile, DestFileT destFile) {
    // for s3; considered last-modified, but it's really object-creation time.
    // also considered e-tag, but it's calculated differently for large (> 16MB) files.
    // file size is good enough?
    return sourceFile.isDirectory() != destFile.isDirectory()
        || sourceFile.size() != destFile.size();
  }

  /** {@code true} if {@link #delete} is necessary. */
  @ForOverride
  protected boolean needDelete(Optional<SourceFileT> optionalSourceFile, DestFileT destFile) {
    return optionalSourceFile.isEmpty();
  }

  @Override
  public final String toString() {
    return "%s[source=%s, destination=%s]"
        .formatted(getClass().getSimpleName(), source, destination);
  }
}
