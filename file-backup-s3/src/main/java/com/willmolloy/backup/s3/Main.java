package com.willmolloy.backup.s3;

import static com.willmolloy.backup.util.Preconditions.require;
import static java.util.Objects.requireNonNull;

import com.willmolloy.backup.BackupRunner;
import com.willmolloy.backup.local.LocalStorage;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.time.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;

/**
 * Main entrypoint.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
final class Main {

  private static final Logger log = LogManager.getLogger();

  /** Main method. */
  public static void main(String... args) {
    try (S3Client s3Client =
            S3Client.builder().region(Region.US_EAST_1).forcePathStyle(true).build();
        S3Waiter s3Waiter =
            S3Waiter.builder()
                .client(s3Client)
                .overrideConfiguration(config -> config.waitTimeout(Duration.ofHours(1)))
                .build()) {
      String sourcePath = readEnvVariable("SOURCE_PATH");
      String destBucket = readEnvVariable("DESTINATION_BUCKET");
      String destPrefix = readEnvVariable("DESTINATION_BUCKET_PREFIX");

      FileSystem fs = FileSystems.getDefault();

      LocalStorage source = new LocalStorage(fs.getPath(sourcePath));
      require(destPrefix.endsWith("/"), "Requires bucket prefix to end with '/': " + destPrefix);
      S3Bucket dest = new S3Bucket(s3Client, destBucket, fs.getPath(destPrefix));

      S3Backup s3Backup = new S3Backup(s3Client, s3Waiter, source, dest);
      if (!BackupRunner.run(s3Backup)) {
        System.exit(1);
      }
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
