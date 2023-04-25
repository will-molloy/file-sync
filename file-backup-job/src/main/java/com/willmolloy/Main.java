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
    JobRunner documentsBackup =
        new JobRunner(
            new FileSystemToFileSystemJob(
                Path.of("C:\\Users\\Will\\Documents"),
                Path.of("D:\\Backups\\windows_home_folder_backup\\Documents")));

    try {
      documentsBackup.run();
    } catch (Throwable t) {
      log.fatal("Fatal error", t);
      System.exit(1);
    }
  }

  private Main() {}
}
