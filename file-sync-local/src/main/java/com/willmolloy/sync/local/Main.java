package com.willmolloy.sync.local;

import static com.willmolloy.sync.util.EnvHelper.getOptionalEnvVariable;
import static com.willmolloy.sync.util.EnvHelper.getRequiredEnvVariable;

import com.willmolloy.sync.statistics.BackupObserver;
import com.willmolloy.sync.statistics.LoggingBackupObserver;
import com.willmolloy.sync.statistics.discord.DiscordWebhook;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.List;

/**
 * Main entrypoint.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
final class Main {

  /** Main method. */
  public static void main(String... args) {
    String sourcePath = getRequiredEnvVariable("SOURCE_PATH");
    String destPath = getRequiredEnvVariable("DESTINATION_PATH");

    FileSystem fs = FileSystems.getDefault();

    LocalStorage source = new LocalStorage(fs.getPath(sourcePath));
    LocalStorage dest = new LocalStorage(fs.getPath(destPath));

    List<BackupObserver> observers = new ArrayList<>();
    observers.add(new LoggingBackupObserver());
    getOptionalEnvVariable("DISCORD_WEBHOOK")
        .ifPresent(webhookUrl -> observers.add(new DiscordWebhook(webhookUrl)));

    LocalBackup localBackup = new LocalBackup(source, dest, observers);
    if (!localBackup.run()) {
      System.exit(1);
    }
  }

  private Main() {}
}
