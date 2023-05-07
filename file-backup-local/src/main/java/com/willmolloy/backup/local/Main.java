package com.willmolloy.backup.local;

import static com.willmolloy.backup.util.Preconditions.require;

import com.willmolloy.backup.BackupRunner;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Arrays;
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
    try {
      require(args.length == 2, "Requires 2 args: " + Arrays.toString(args));

      FileSystem fs = FileSystems.getDefault();

      Path sourceRoot = fs.getPath(args[0]);
      Path destRoot = fs.getPath(args[1]);

      LocalStorage source = new LocalStorage(sourceRoot);
      LocalStorage dest = new LocalStorage(destRoot);

      LocalBackup backup = new LocalBackup(source, dest);
      new BackupRunner(backup).run();

    } catch (Throwable t) {
      log.fatal("Fatal error", t);
      System.exit(1);
    }
  }

  private Main() {}
}
