package com.willmolloy.backup.s3;

import static com.willmolloy.backup.util.Md5Helper.md5Base64;
import static com.willmolloy.backup.util.PathHelper.ensureUnixSeparator;
import static java.util.Objects.requireNonNull;

import com.willmolloy.backup.Backup;
import com.willmolloy.backup.File;
import com.willmolloy.backup.local.LocalStorage;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.core.sync.RequestBody;
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
  public boolean put(File sourceFile) {
    Path key = sourceFile.relativizedPath();
    Path sourcePath = source.root().resolve(key);
    String destinationUri =
        sourceFile.isDirectory() ? destination.folderUri(key) : destination.objectUri(key);
    try {
      PutObjectRequest.Builder baseRequest =
          PutObjectRequest.builder()
              .bucket(destination.bucketName())
              .key(
                  ensureUnixSeparator(destination.prefix().resolve(key))
                      + (sourceFile.isDirectory() ? "/" : ""))
              .storageClass(StorageClass.DEEP_ARCHIVE);

      if (!sourceFile.isDirectory()) {
        // TODO multipart upload for large files
        PutObjectRequest request = baseRequest.contentMD5(md5Base64(sourcePath)).build();
        s3Client.putObject(request, sourcePath);
      } else {
        PutObjectRequest request = baseRequest.build();
        s3Client.putObject(request, RequestBody.empty());
      }
      // TODO waiter
      log.info("Put: [{}] -> [{}]", sourcePath, destinationUri);
      return true;
    } catch (NoSuchFileException e) {
      log.warn(
          "Skipped put: [{}] -> [{}]. Source file deleted since scan",
          sourcePath,
          destinationUri,
          e);
      return true;
    } catch (RuntimeException | IOException e) {
      log.error("Error putting: [{}] -> [{}]", sourcePath, destinationUri, e);
      return false;
    }
  }

  @Override
  public boolean delete(File destFile) {
    Path key = destFile.relativizedPath();
    String destinationUri =
        destFile.isDirectory() ? destination.folderUri(key) : destination.objectUri(key);
    try {
      DeleteObjectRequest request =
          DeleteObjectRequest.builder()
              .bucket(destination.bucketName())
              .key(
                  ensureUnixSeparator(destination.prefix().resolve(key))
                      + (destFile.isDirectory() ? "/" : ""))
              .build();
      s3Client.deleteObject(request);
      // TODO waiter
      log.info("Deleted: [{}]", destinationUri);
      return true;
    } catch (RuntimeException e) {
      log.error("Error deleting: [{}]", destinationUri, e);
      return false;
    }
  }

  @Override
  public String toString() {
    return "%s[source=%s, destination=%s]"
        .formatted(getClass().getSimpleName(), source, destination);
  }
}
