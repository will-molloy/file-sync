package com.willmolloy.backup;

import static com.willmolloy.backup.util.Preconditions.require;
import static java.util.Objects.requireNonNull;

import com.willmolloy.backup.Backup.Node;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;
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
  // static factory ensures single instance per run
  public static OverallStatistics run(Backup<?, ?> backup) {
    return new BackupRunner(backup).run();
  }

  private final AtomicInteger createCount = new AtomicInteger();
  private final AtomicInteger failedCreateCount = new AtomicInteger();
  private final AtomicInteger updateCount = new AtomicInteger();
  private final AtomicInteger failedUpdateCount = new AtomicInteger();
  private final AtomicInteger deleteCount = new AtomicInteger();
  private final AtomicInteger failedDeleteCount = new AtomicInteger();
  private final AtomicInteger sameCount = new AtomicInteger();
  // long can represent up to 9.2 EB
  private final AtomicLong bytesAdded = new AtomicLong();
  private final AtomicLong bytesRemoved = new AtomicLong();

  private final Backup<?, ?> backup;

  private BackupRunner(Backup<?, ?> backup) {
    this.backup = requireNonNull(backup);
  }

  private OverallStatistics run() {
    log.info("Running: {}", backup);
    long runStartNanos = System.nanoTime();

    try (ExecutorService threadPool =
        Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("worker-", 1).factory())) {

      TreeMap<String, Node> sourceNodes = scanWithLog(backup.source()::scan, "source");
      TreeMap<String, Node> destNodes = scanWithLog(backup.destination()::scan, "destination");

      sourceNodes.forEach(
          (key, sourceNode) -> {
            // TODO WIP - minimal ops...
            String nextKey = sourceNodes.higherKey(key);
            // TODO it needs to check it's a prefix of PATH i.e. until a '/'...
            if (nextKey != null && nextKey.startsWith(key)) {
              // 'nextKey' maps to a child of 'key'
              // skip put as put of child will cover this node
              return;
            }

            switch (sourceNode) {
              case Node.File sourceFile -> {
                Node destNode = destNodes.get(key);
                if (destNode == null || destNode instanceof Node.Directory) {
                  threadPool.submit(() -> create(key, sourceFile));
                } else if (destNode instanceof Node.File destFile && !sourceFile.same(destFile)) {
                  threadPool.submit(() -> update(key, sourceFile, destFile));
                } else {
                  sameCount.incrementAndGet();
                }
              }
              case Node.Directory sourceDir -> {
                // limit to files only for now
                // TODO what about backing up empty dirs (i.e. leaves)?
                //  Would require expensive IO call to test 'is empty dir'...
              }
            }
          });

      // for dest we need to attempt deletion of every path, including directories
      // otherwise empty dirs are left on destination
      destNodes.forEach(
          (key, destFile) -> {
            if (sourceNodes.containsKey(key)) {
              return;
            }

            // TODO WIP - minimal ops...
            // TODO just make scan key a Path?
            int parentI = key.lastIndexOf('/');
            if (parentI > 0) {
              String parent = key.substring(0, key.lastIndexOf('/'));
              if (destNodes.containsKey(parent)) {
                // skip... deletion of parent will cover this node
                return;
              }
            }

            if (!sourceNodes.containsKey(key)) {
              threadPool.submit(() -> delete(key, destFile));
            }
          });
    }
    return getAndLogStats(runStartNanos);
  }

  private TreeMap<String, Node> scanWithLog(
      Supplier<Map<String, Node>> scan, String locationForLog) {
    long scanStartNanos = System.nanoTime();

    TreeMap<String, Node> nodes = new TreeMap<>(scan.get());

    List<Node.File> files =
        nodes.values().stream()
            .flatMap(node -> node instanceof Node.File file ? Stream.of(file) : Stream.empty())
            .toList();
    long sizeMB = files.stream().mapToLong(Node.File::size).sum() / MEGA;
    log.info(
        "Scanned {} in: {}. {} files. {}MB",
        locationForLog,
        elapsed(scanStartNanos),
        NUMBER_FORMAT.format(files.size()),
        NUMBER_FORMAT.format(sizeMB));

    return nodes;
  }

  private void create(String key, Node.File sourceFile) {
    if (backup.put(key)) {
      createCount.incrementAndGet();
      bytesAdded.addAndGet(sourceFile.size());
    } else {
      failedCreateCount.incrementAndGet();
    }
  }

  private void update(String key, Node.File sourceFile, Node.File destFile) {
    if (backup.put(key)) {
      updateCount.incrementAndGet();
      bytesAdded.addAndGet(sourceFile.size());
      bytesRemoved.addAndGet(destFile.size());
    } else {
      failedUpdateCount.incrementAndGet();
    }
  }

  private void delete(String key, Node destNode) {
    if (backup.delete(key)) {
      if (destNode instanceof Node.File destFile) {
        deleteCount.incrementAndGet();
        bytesRemoved.addAndGet(destFile.size());
      }
    } else {
      failedDeleteCount.incrementAndGet();
    }
  }

  private OverallStatistics getAndLogStats(long runStartNanos) {
    Statistics statistics =
        new Statistics(
            createCount.get(),
            updateCount.get(),
            deleteCount.get(),
            sameCount.get(),
            bytesAdded.get(),
            bytesRemoved.get());
    ErrorStatistics errorStatistics =
        new ErrorStatistics(
            failedCreateCount.get(), failedUpdateCount.get(), failedDeleteCount.get());

    log.info("Finished: {} in: {}. {}", backup, elapsed(runStartNanos), statistics);
    if (errorStatistics.any()) {
      log.warn("{}", errorStatistics);
    }

    return new OverallStatistics(statistics, errorStatistics);
  }

  private static Duration elapsed(long startNanos) {
    return Duration.ofNanos(System.nanoTime() - startNanos);
  }

  /**
   * Statistics over a backup run.
   *
   * @param filesCreated files created on destination
   * @param filesUpdated files updated on destination
   * @param filesDeleted files deleted on destination
   * @param filesSame files kept same
   * @param bytesAdded bytes added to destination
   * @param bytesRemoved bytes removed from destination
   */
  public record Statistics(
      int filesCreated,
      int filesUpdated,
      int filesDeleted,
      int filesSame,
      long bytesAdded,
      long bytesRemoved) {
    public Statistics {
      require(filesCreated >= 0);
      require(filesUpdated >= 0);
      require(filesDeleted >= 0);
      require(filesSame >= 0);
      require(bytesAdded >= 0);
      require(bytesRemoved >= 0);
    }

    @Override
    public String toString() {
      return "%s files created, %s files updated, %s files deleted, %s files same. %sMB added, %sMB removed"
          .formatted(
              NUMBER_FORMAT.format(filesCreated),
              NUMBER_FORMAT.format(filesUpdated),
              NUMBER_FORMAT.format(filesDeleted),
              NUMBER_FORMAT.format(filesSame),
              NUMBER_FORMAT.format(bytesAdded / MEGA),
              NUMBER_FORMAT.format(bytesRemoved / MEGA));
    }
  }

  /**
   * Error statistics over a backup run.
   *
   * @param failedCreates failed creates
   * @param failedUpdates failed updates
   * @param failedDeletes failed deletes
   */
  public record ErrorStatistics(int failedCreates, int failedUpdates, int failedDeletes) {
    public ErrorStatistics {
      require(failedCreates >= 0);
      require(failedUpdates >= 0);
      require(failedDeletes >= 0);
    }

    public boolean any() {
      return IntStream.of(failedCreates, failedUpdates, failedDeletes).anyMatch(i -> i > 0);
    }

    @Override
    public String toString() {
      return "%s failed creates, %s failed updates, %s failed deletes"
          .formatted(
              NUMBER_FORMAT.format(failedCreates),
              NUMBER_FORMAT.format(failedUpdates),
              NUMBER_FORMAT.format(failedDeletes));
    }
  }

  /**
   * Overall statistics over a backup run.
   *
   * @param statistics statistics
   * @param errorStatistics error statistics
   */
  public record OverallStatistics(Statistics statistics, ErrorStatistics errorStatistics) {
    public OverallStatistics {
      requireNonNull(statistics);
      requireNonNull(errorStatistics);
    }
  }
}
