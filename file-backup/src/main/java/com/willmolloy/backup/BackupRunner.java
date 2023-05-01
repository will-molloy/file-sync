package com.willmolloy.backup;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import com.willmolloy.backup.Backup.File;
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
  private final Backup.Source source;
  private final Backup.Destination destination;

  BackupRunner(Backup backup) {
    this.backup = requireNonNull(backup);
    this.source = backup.source();
    this.destination = backup.destination();
  }

  void run() {
    log.info("Running: {}", backup);
    try (ExecutorService executorService =
        Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("worker-", 1).factory())) {
      ops().forEach(executorService::submit);
    }
    log.info("Finished: {}", backup);
  }

  private Stream<Runnable> ops() {
    Map<String, File> sourceFiles = source.scan();
    Map<String, File> destFiles = destination.scan();

    Stream<Runnable> copiesOrUpdates = sourceFiles.entrySet().stream().map(e -> () -> {
      String key = e.getKey();

      File sourceFile = e.getValue();
      File destFile = destFiles.get(key);

      Path sourcePath = source.get(key);

      if (destFile == null) {
        destination.put(key, sourcePath);
      } else if (!equals(sourceFile, destFile)) {
        destination.put(key, sourcePath);
      }
    });

    Stream<Runnable> deletes = destFiles.keySet().stream().map(key -> () -> {
      if (!sourceFiles.containsKey(key)) {
        destination.delete(key);
      }
    });

    return Stream.concat(copiesOrUpdates, deletes);
  }

  private boolean equals(File sourceFile, File destFile) {
    return sourceFile.sizeInBytes() == destFile.sizeInBytes()
        && sourceFile.lastModified().equals(destFile.lastModified());
  }

  private record Statistics(long copies, long updates, long deletes) {}
}
