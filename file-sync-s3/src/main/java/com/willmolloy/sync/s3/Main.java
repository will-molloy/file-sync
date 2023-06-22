package com.willmolloy.sync.s3;

import static com.google.common.base.Preconditions.checkArgument;
import static com.willmolloy.sync.util.EnvHelper.getOptionalEnvVariable;
import static com.willmolloy.sync.util.EnvHelper.getRequiredEnvVariable;

import com.willmolloy.sync.local.LocalStorage;
import com.willmolloy.sync.statistics.LoggingSyncObserver;
import com.willmolloy.sync.statistics.SyncObserver;
import com.willmolloy.sync.statistics.discord.DiscordWebhook;
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

      List<SyncObserver> observers = new ArrayList<>();
      observers.add(new LoggingSyncObserver());
      getOptionalEnvVariable("DISCORD_WEBHOOK")
          .ifPresent(webhookUrl -> observers.add(new DiscordWebhook(webhookUrl)));

      S3Sync s3Sync = new S3Sync(s3Client, s3Waiter, source, dest, observers);
      if (!s3Sync.run()) {
        System.exit(1);
      }
    }
  }

  private Main() {}
}
