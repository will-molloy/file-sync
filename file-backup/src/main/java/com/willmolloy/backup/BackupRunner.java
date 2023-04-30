package com.willmolloy.backup;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

  private final Backup backup;
  private final Backup.Location source;
  private final Backup.Location destination;

  BackupRunner(Backup backup) {
    this.backup = requireNonNull(backup);
    this.source = backup.source();
    this.destination = backup.destination();
  }

  void run() {
    log.info("running {}", backup);
    try (ExecutorService executorService =
        Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("worker-", 1).factory())) {
      ops().forEach(executorService::submit);
    }
  }

  private Stream<Runnable> ops() {
    Stream<Runnable> copiesAndUpdates =
        backup.source().scan().map(relativePath -> () -> tryCopyOrUpdate(relativePath));

    Stream<Runnable> deletes =
        backup.destination().scan().map(relativePath -> () -> tryDelete(relativePath));

    return Stream.concat(copiesAndUpdates, deletes);
  }

  private void tryCopyOrUpdate(Path relativePath) {
    if (!destination.exists(relativePath)) {
      backup.copy(relativePath);
    } else {
      boolean sourceIsDirectory = source.isDirectory(relativePath);
      boolean destIsDirectory = destination.isDirectory(relativePath);
      if (sourceIsDirectory && destIsDirectory) {
        return;
      } else if (sourceIsDirectory != destIsDirectory) {
        // if the file is a directory on dest, need to delete it first
        backup.delete(relativePath);
        backup.copy(relativePath);
      } else if (!equals(relativePath)) {
        backup.update(relativePath);
      }
    }
  }

  private void tryDelete(Path relativePath) {
    if (!source.exists(relativePath)) {
      backup.delete(relativePath);
    }
  }

  private boolean equals(Path relativePath) {
    return source.size(relativePath) == destination.size(relativePath)
        && source.lastModified(relativePath) == destination.lastModified(relativePath);
  }
}
