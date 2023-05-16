package com.willmolloy.backup.s3;

import static com.willmolloy.backup.s3.S3Bucket.ensureUnixSeparator;
import static com.willmolloy.backup.util.Md5Helper.md5Base64;
import static java.util.Objects.requireNonNull;

import com.willmolloy.backup.Backup;
import com.willmolloy.backup.local.LocalStorage;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.StorageClass;

/**
 * For backups to AWS S3.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
class S3Backup implements Backup<LocalStorage, S3Bucket> {

  private static final Logger log = LogManager.getLogger();

  private final S3Client s3Client;

  private final LocalStorage source;
  private final S3Bucket destination;

  S3Backup(S3Client s3Client, LocalStorage source, S3Bucket destination) {
    this.s3Client = requireNonNull(s3Client);
    this.source = requireNonNull(source);
    this.destination = requireNonNull(destination);
  }

  @Override
  public LocalStorage source() {
    return source;
  }

  @Override
  public S3Bucket destination() {
    return destination;
  }

  @Override
  public boolean put(Path key) {
    Path sourcePath = source.root().resolve(key);
    String destinationUri = destination.objectUri(key);
    try {
      // TODO multipart upload for large files
      PutObjectRequest request =
          PutObjectRequest.builder()
              .bucket(destination.bucketName())
              .key(ensureUnixSeparator(destination.prefix().resolve(key)))
              .contentMD5(md5Base64(sourcePath))
              .storageClass(StorageClass.DEEP_ARCHIVE)
              .build();
      s3Client.putObject(request, sourcePath);
      log.info("Put: [{}] -> [{}]", sourcePath, destinationUri);
      return true;
    } catch (NoSuchFileException ignored) {
      log.warn(
          "Skipped put: [{}] -> [{}]. Source file deleted since scan", sourcePath, destinationUri);
      return true;
    } catch (RuntimeException | IOException e) {
      log.error("Error putting: [%s] -> [%s]".formatted(sourcePath, destinationUri), e);
      return false;
    }
  }

  @Override
  public boolean delete(Path key) {
    String destinationUri = destination.objectUri(key);
    try {
      DeleteObjectRequest request =
          DeleteObjectRequest.builder()
              .bucket(destination.bucketName())
              .key(ensureUnixSeparator(destination.prefix().resolve(key)))
              .build();
      s3Client.deleteObject(request);
      log.info("Deleted: [{}]", destinationUri);
      return true;
    } catch (RuntimeException e) {
      log.error("Error deleting: [%s]".formatted(destinationUri), e);
      return false;
    }
  }

  @Override
  public String toString() {
    return "S3Backup[source=%s, destination=%s]".formatted(source, destination);
  }
}
