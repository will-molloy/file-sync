package com.willmolloy.backup;

import com.willmolloy.backup.local.LocalBackup;
import com.willmolloy.backup.local.LocalStorage;
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
    log.debug("main({})", (Object) args);
    try {
      Path sourceRoot = Path.of(args[0]);
      Path destRoot = Path.of(args[1]);

      LocalStorage source = new LocalStorage(sourceRoot);
      LocalStorage destination = new LocalStorage(destRoot);
      LocalBackup backup = new LocalBackup(source, destination);
      new BackupRunner(backup).run();
    } catch (Throwable t) {
      log.fatal("Fatal error", t);
      System.exit(1);
    }
  }

  private Main() {}
}
