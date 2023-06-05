package com.willmolloy.backup.local;

import static com.willmolloy.backup.util.EnvHelper.readOptionalEnvVariable;
import static com.willmolloy.backup.util.EnvHelper.readRequiredEnvVariable;

import com.willmolloy.backup.statistics.BackupObserver;
import com.willmolloy.backup.statistics.DiscordWebhook;
import com.willmolloy.backup.statistics.LoggingBackupObserver;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.time.Duration;
import java.util.ArrayList;
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
  public static void main(String... args) throws InterruptedException {
    try {
      String sourcePath = readRequiredEnvVariable("SOURCE_PATH");
      String destPath = readRequiredEnvVariable("DESTINATION_PATH");

      FileSystem fs = FileSystems.getDefault();

      LocalStorage source = new LocalStorage(fs.getPath(sourcePath));
      LocalStorage dest = new LocalStorage(fs.getPath(destPath));

      List<BackupObserver> observers = new ArrayList<>();
      observers.add(new LoggingBackupObserver());
      readOptionalEnvVariable("DISCORD_WEBHOOK")
          .ifPresent(webhookUrl -> observers.add(new DiscordWebhook(webhookUrl)));

      LocalBackup localBackup = new LocalBackup(source, dest, observers);
      if (!localBackup.run()) {
        System.exit(1);
      }
    } catch (Throwable t) {
      log.fatal("Fatal error", t);
      Thread.sleep(Duration.ofHours(1));
      System.exit(1);
    }
  }

  private Main() {}
}
