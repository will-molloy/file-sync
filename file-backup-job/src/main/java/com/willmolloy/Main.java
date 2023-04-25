package com.willmolloy;

import java.nio.file.Path;
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
    try {
      Path sourceRoot = Path.of(args[0]);
      Path destRoot = Path.of(args[1]);

      JobRunner backup = new JobRunner(new FileSystemToFileSystemJob(sourceRoot, destRoot));

      backup.run();
    } catch (Throwable t) {
      log.fatal("Fatal error", t);
      System.exit(1);
    }
  }

  private Main() {}
}
