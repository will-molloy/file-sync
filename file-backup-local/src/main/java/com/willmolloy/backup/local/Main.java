package com.willmolloy.backup.local;

import static java.util.Objects.requireNonNull;

import com.willmolloy.backup.BackupRunner;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
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
    try {
      String sourcePath = readEnvVariable("SOURCE_PATH");
      String destPath = readEnvVariable("DESTINATION_PATH");

      FileSystem fs = FileSystems.getDefault();

      LocalStorage source = new LocalStorage(fs.getPath(sourcePath));
      LocalStorage dest = new LocalStorage(fs.getPath(destPath));

      LocalBackup localBackup = new LocalBackup(source, dest);
      if (!BackupRunner.run(localBackup)) {
        System.exit(1);
      }
    } catch (Throwable t) {
      log.fatal("Fatal error", t);
      System.exit(1);
    }
  }

  private static String readEnvVariable(String name) {
    return requireNonNull(System.getenv(name), "Missing %s".formatted(name));
  }

  private Main() {}
}
