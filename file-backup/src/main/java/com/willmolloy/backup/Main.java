package com.willmolloy.backup;

import java.nio.file.Path;
import java.time.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Main entrypoint.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
final class Main {

  private static final Logger log = LogManager.getLogger();

  /** Main method. */
  public static void main(String... args) {
    log.debug("main({})", (Object) args);
    long start = System.nanoTime();
    try {
      Path sourceRoot = Path.of(args[0]);
      Path destRoot = Path.of(args[1]);

      BackupRunner backupRunner = new BackupRunner(new FileSystemBackup(sourceRoot, destRoot));
      backupRunner.run();
    } catch (Throwable t) {
      log.fatal("Fatal error", t);
      System.exit(1);
    } finally {
      Duration elapsed = Duration.ofNanos(System.nanoTime() - start);
      log.info("Elapsed: {}", elapsed);
    }
  }

  private Main() {}
}
