package com.willmolloy.backup.s3;

import static java.util.Objects.requireNonNull;

import com.willmolloy.backup.BackupRunner;
import com.willmolloy.backup.local.LocalStorage;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

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
        S3Client.builder().region(Region.US_EAST_1).forcePathStyle(true).build()) {
      String sourcePath = readEnvVariable("SOURCE_PATH");
      String destBucket = readEnvVariable("DESTINATION_BUCKET");
      String destPrefix = readEnvVariable("DESTINATION_BUCKET_PREFIX");

      FileSystem fs = FileSystems.getDefault();

      LocalStorage source = new LocalStorage(fs.getPath(sourcePath));
      S3Bucket dest = new S3Bucket(s3Client, destBucket, destPrefix);

      S3Backup backup = new S3Backup(s3Client, source, dest);
      BackupRunner.OverallStatistics statistics = new BackupRunner(backup).run();

      if (statistics.errorStatistics().any()) {
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
