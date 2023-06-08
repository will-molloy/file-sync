package com.willmolloy.backup.local;

import static com.google.common.base.Preconditions.checkNotNull;

import com.willmolloy.backup.statistics.LoggingBackupObserver;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.List;
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

      LocalBackup localBackup = new LocalBackup(source, dest, List.of(new LoggingBackupObserver()));
      if (!localBackup.run()) {
        System.exit(1);
      }
    } catch (Throwable t) {
      log.fatal("Fatal error", t);
      System.exit(1);
    }
  }

  private static String readEnvVariable(String name) {
    return checkNotNull(System.getenv(name), "Missing: %s", name);
  }

  private Main() {}
}
