package com.willmolloy.backup.s3;

import static com.google.common.base.Preconditions.checkArgument;
import static com.willmolloy.backup.util.EnvHelper.getOptionalEnvVariable;
import static com.willmolloy.backup.util.EnvHelper.getRequiredEnvVariable;

import com.willmolloy.backup.local.LocalStorage;
import com.willmolloy.backup.statistics.BackupObserver;
import com.willmolloy.backup.statistics.LoggingBackupObserver;
import com.willmolloy.backup.statistics.discord.DiscordWebhook;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;

/**
 * Main entrypoint.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
final class Main {

  /** Main method. */
  public static void main(String... args) {
    try (S3Client s3Client =
            S3Client.builder().region(Region.US_EAST_1).forcePathStyle(true).build();
        S3Waiter s3Waiter =
            S3Waiter.builder()
                .client(s3Client)
                .overrideConfiguration(config -> config.waitTimeout(Duration.ofHours(1)))
                .build()) {
      String sourcePath = getRequiredEnvVariable("SOURCE_PATH");
      String destBucket = getRequiredEnvVariable("DESTINATION_BUCKET");
      String destPrefix = getRequiredEnvVariable("DESTINATION_BUCKET_PREFIX");

      FileSystem fs = FileSystems.getDefault();

      LocalStorage source = new LocalStorage(fs.getPath(sourcePath));
      checkArgument(
          destPrefix.endsWith("/"), "Requires bucket prefix to end with '/': " + destPrefix);
      S3Bucket dest = new S3Bucket(s3Client, destBucket, fs.getPath(destPrefix));

      List<BackupObserver> observers = new ArrayList<>();
      observers.add(new LoggingBackupObserver());
      getOptionalEnvVariable("DISCORD_WEBHOOK")
          .ifPresent(webhookUrl -> observers.add(new DiscordWebhook(webhookUrl)));

      S3Backup s3Backup = new S3Backup(s3Client, s3Waiter, source, dest, observers);
      if (!s3Backup.run()) {
        System.exit(1);
      }
    }
  }

  private Main() {}
}
