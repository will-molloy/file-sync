package com.willmolloy.backup;

import static java.util.Objects.requireNonNull;

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

  BackupRunner(Backup backup) {
    this.backup = requireNonNull(backup);
  }

  void run() {
    log.info("running {}", backup);
    try (ExecutorService executorService =
        Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("worker-", 1).factory())) {
      ops().forEach(executorService::submit);
    }
  }

  private Stream<Runnable> ops() {
    Stream<Runnable> copies =
        backup.source().scan().map(sourcePath -> () -> backup.tryCopyOrUpdate(sourcePath));

    Stream<Runnable> deletes =
        backup.destination().scan().map(destPath -> () -> backup.tryDelete(destPath));

    return Stream.concat(copies, deletes);
  }
}
