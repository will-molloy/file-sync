package com.willmolloy.backup.s3;

import static com.willmolloy.backup.util.Preconditions.require;

import com.willmolloy.backup.BackupRunner;
import com.willmolloy.backup.local.LocalStorage;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Arrays;
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
    log.debug("main({})", (Object) args);

    try (S3Client s3Client =
        S3Client.builder().region(Region.US_EAST_1).forcePathStyle(true).build()) {
      require(args.length == 3, "Requires 3 args: " + Arrays.toString(args));

      FileSystem fs = FileSystems.getDefault();

      Path sourceRoot = fs.getPath(args[0]);
      String destBucket = args[1];
      String destPrefix = args[2];

      LocalStorage source = new LocalStorage(sourceRoot);
      S3Bucket dest = new S3Bucket(s3Client, destBucket, destPrefix);

      S3Backup backup = new S3Backup(s3Client, source, dest);
      new BackupRunner(backup).run();

    } catch (Throwable t) {
      log.fatal("Fatal error", t);
      System.exit(1);
    }
  }

  private Main() {}
}
