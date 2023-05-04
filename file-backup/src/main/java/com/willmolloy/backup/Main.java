package com.willmolloy.backup;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Main entrypoint.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public final class Main {

  private static final Logger log = LogManager.getLogger();

  /** Main method. */
  public static void main(String... args) {
    log.debug("main({})", (Object) args);
    try {
      Backup<?, ?> backup = BackupFactory.create(args);
      new BackupRunner(backup).run();
    } catch (Throwable t) {
      log.fatal("Fatal error", t);
      System.exit(1);
    }
  }

  private Main() {}
}
