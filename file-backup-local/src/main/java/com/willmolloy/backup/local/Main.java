package com.willmolloy.backup.local;

import static java.util.Objects.requireNonNull;

import com.willmolloy.backup.BackupRunner;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
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
      Path sourceRoot = fs.getPath(sourcePath);
      Path destRoot = fs.getPath(destPath);

      LocalStorage source = new LocalStorage(sourceRoot);
      LocalStorage dest = new LocalStorage(destRoot);

      LocalBackup backup = new LocalBackup(source, dest);
      new BackupRunner(backup).run();

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
