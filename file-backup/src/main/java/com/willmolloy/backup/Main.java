package com.willmolloy.backup;

import com.willmolloy.backup.filesystem.FileSystem;
import com.willmolloy.backup.filesystem.FileSystemBackup;
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
    long startNanos = System.nanoTime();
    try {
      Path sourceRoot = Path.of(args[0]);
      Path destRoot = Path.of(args[1]);

      FileSystem source = new FileSystem(sourceRoot);
      FileSystem destination = new FileSystem(destRoot);
      FileSystemBackup backup = new FileSystemBackup(source, destination);
      BackupRunner backupRunner = new BackupRunner(backup);
      backupRunner.run();
    } catch (Throwable t) {
      log.fatal("Fatal error", t);
      System.exit(1);
    } finally {
      Duration elapsed = Duration.ofNanos(System.nanoTime() - startNanos);
      log.info("Elapsed: {}", elapsed);
    }
  }

  private Main() {}
}
