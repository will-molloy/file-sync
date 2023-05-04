package com.willmolloy.backup;

import static com.willmolloy.backup.util.Preconditions.check;

import com.willmolloy.backup.local.LocalBackup;
import com.willmolloy.backup.local.LocalStorage;
import com.willmolloy.backup.s3.S3Backup;
import com.willmolloy.backup.s3.S3Bucket;
import java.nio.file.Path;
import java.util.Arrays;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Factory for creating instances of {@link Backup}.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
final class BackupFactory {

  static Backup<?, ?> create(String... args) {
    check(args.length >= 1, "Expected at least 1 arg");
    String type = args[0];
    return switch (type) {
      case "S3Backup" -> createS3Backup(args);
      case "LocalBackup" -> createLocalBackup(args);
      default -> throw new IllegalArgumentException("Unknown backup type: %s".formatted(type));
    };
  }

  private static LocalBackup createLocalBackup(String... args) {
    check(args.length == 3, "Expected 3 args: " + Arrays.toString(args));
    Path sourceRoot = Path.of(args[1]);
    Path destRoot = Path.of(args[2]);

    LocalStorage source = new LocalStorage(sourceRoot);
    LocalStorage dest = new LocalStorage(destRoot);
    return new LocalBackup(source, dest);
  }

  private static S3Backup createS3Backup(String... args) {
    check(args.length == 4, "Expected 4 args: " + Arrays.toString(args));
    Path sourceRoot = Path.of(args[1]);
    String destBucket = args[2];
    String destPrefix = args[3];

    S3Client s3Client = S3Client.builder().region(Region.US_EAST_1).forcePathStyle(true).build();
    LocalStorage source = new LocalStorage(sourceRoot);
    S3Bucket dest = new S3Bucket(s3Client, destBucket, destPrefix);
    return new S3Backup(s3Client, source, dest);
  }

  private BackupFactory() {}
}
