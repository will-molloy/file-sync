package com.willmolloy.backup;

import static com.willmolloy.backup.util.TimeHelper.elapsed;
import static java.util.Objects.requireNonNull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
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
public abstract class BaseBackup<SourceFileT extends File, DestFileT extends File> {
  private static final Logger log = LogManager.getLogger();
  private static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance(Locale.ENGLISH);
  private static final int MEGA = 1_000_000;

  private static ExecutorService threadPool(String name) {
    return Executors.newThreadPerTaskExecutor(
        Thread.ofVirtual().name("%s-worker-".formatted(name), 1).factory());
  }

  private final Location<SourceFileT> source;
  private final Location<DestFileT> destination;

  protected BaseBackup(Location<SourceFileT> source, Location<DestFileT> destination) {
    this.source = requireNonNull(source);
    this.destination = requireNonNull(destination);
  }

  /** Runs the backup. */
  public final boolean run() {
    log.info("Running: {}", this);
    long startNanos = System.nanoTime();

    AtomicInteger puts = new AtomicInteger();
    AtomicInteger deletes = new AtomicInteger();
    AtomicInteger failedPuts = new AtomicInteger();
    AtomicInteger failedDeletes = new AtomicInteger();
    AtomicLong bytesAdded = new AtomicLong();
    // TODO doesn't include cases where file is updated
    //  - but shouldn't count create after delete as update
    AtomicLong bytesRemoved = new AtomicLong();

    FileTree<SourceFileT> sourceFileTree = source.fileTree();
    FileTree<DestFileT> destFileTree = destination.fileTree();

    // Doing deletes first; otherwise there are scenarios where put can fail, e.g. non-empty dir
    // overwriting a file
    try (ExecutorService threadPool = threadPool("delete")) {
      destFileTree
          .postorder()
          .filter(this::needDelete)
          // skip delete if covered by ancestor, since children are deleted too
          .filter(destFile -> destFileTree.ancestors(destFile).noneMatch(this::needDelete))
          .forEach(
              destFile ->
                  threadPool.submit(
                      () -> {
                        log.debug("delete({})", destFile);
                        if (delete(destFile)) {
                          FileTree<DestFileT> subtree = destFileTree.subtree(destFile);
                          deletes.addAndGet((int)subtree.fileCount());
                          bytesRemoved.addAndGet(subtree.totalSize());
                        } else {
                          failedDeletes.incrementAndGet();
                        }
                      }));
    }

    try (ExecutorService threadPool = threadPool("put")) {
      sourceFileTree
          // only need to put leaves, parents are created as necessary
          .leaves()
          .filter(this::needPut)
          .forEach(
              sourceFile ->
                  threadPool.submit(
                      () -> {
                        log.debug("put({})", sourceFile);
                        if (put(sourceFile)) {
                          puts.incrementAndGet();
                          bytesAdded.addAndGet(sourceFile.size());
                        } else {
                          failedPuts.incrementAndGet();
                        }
                      }));
    }

    log.info(
        "Finished: {} in: {}. {} puts, {} deletes. {}MB added, {}MB removed",
        this,
        elapsed(startNanos),
        puts.get(),
        deletes.get(),
        NUMBER_FORMAT.format(bytesAdded.get() / MEGA),
        NUMBER_FORMAT.format(bytesRemoved.get() / MEGA));
    boolean allSuccess = IntStream.of(failedPuts.get(), failedDeletes.get()).allMatch(i -> i == 0);
    if (!allSuccess) {
      log.warn("Failed {} puts and {} deletes", failedPuts.get(), failedDeletes.get());
    }
    return allSuccess;
  }

  /**
   * Creates or updates the corresponding file on destination.
   *
   * @return {@code true} if create/update was successful
   * @implSpec Creates parent directories when necessary
   */
  protected abstract boolean put(SourceFileT sourceFile);

  /**
   * Deletes the file on destination.
   *
   * @return {@code true} if delete was successful
   * @implSpec Deletes child directories/files when necessary
   */
  protected abstract boolean delete(DestFileT destFile);

  /** {@code true} if {@link #put} is necessary. */
  protected boolean needPut(SourceFileT sourceFile) {
    FileTree<DestFileT> destFileTree = destination.fileTree();
    Optional<DestFileT> maybeDestFile = destFileTree.get(sourceFile.relativePath());
    // either file not on dest -> create
    // OR files different -> update
    return maybeDestFile.isEmpty() || needUpdate(sourceFile, maybeDestFile.get());
  }

  /** {@code true} if update (via {@link #put}) is necessary. */
  protected boolean needUpdate(SourceFileT sourceFile, DestFileT destFile) {
    // for s3; considered last-modified, but it's really object-creation time.
    // also considered e-tag, but it's calculated differently for large (> 16MB) files.
    // file size is good enough?
    return sourceFile.isDirectory() != destFile.isDirectory()
        || sourceFile.size() != destFile.size();
  }

  /** {@code true} if {@link #delete} is necessary. */
  protected boolean needDelete(DestFileT destFile) {
    FileTree<SourceFileT> sourceFileTree = source.fileTree();
    // file not on source -> delete
    return !sourceFileTree.contains(destFile.relativePath());
  }

  @Override
  public final String toString() {
    return "%s[source=%s, destination=%s]"
        .formatted(getClass().getSimpleName(), source, destination);
  }
}
