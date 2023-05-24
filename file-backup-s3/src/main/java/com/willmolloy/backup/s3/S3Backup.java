package com.willmolloy.backup.s3;

import static com.willmolloy.backup.util.Md5Helper.md5Base64;
import static com.willmolloy.backup.util.PathHelper.ensureUnixSeparator;
import static java.util.Objects.requireNonNull;

import com.willmolloy.backup.BaseBackup;
import com.willmolloy.backup.local.LocalFile;
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
final class S3Backup extends BaseBackup<LocalFile, S3File> {

  private static final Logger log = LogManager.getLogger();

  private final S3Client s3Client;
  private final S3Bucket destination;

  S3Backup(S3Client s3Client, LocalStorage source, S3Bucket destination) {
    super(source, destination);
    this.s3Client = requireNonNull(s3Client);
    this.destination = requireNonNull(destination);
  }

  @Override
  public boolean put(LocalFile sourceFile) {
    Path sourcePath = sourceFile.fullPath();
    Path key = sourceFile.relativePath();
    // TODO put with some kind of filler object so we can use the S3File methods here...
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
      // TODO waiter?
      log.info("Put: [{}] -> [{}]", sourceFile, destinationUri);
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
  public boolean delete(S3File destFile) {
    Path key = destFile.relativePath();
    try {
      // TODO DeleteObjectsRequest for folders... currently does not work.
      DeleteObjectRequest request =
          DeleteObjectRequest.builder()
              .bucket(destination.bucketName())
              .key(
                  ensureUnixSeparator(destination.prefix().resolve(key))
                      + (destFile.isDirectory() ? "/" : ""))
              .build();
      s3Client.deleteObject(request);
      // TODO waiter?
      log.info("Deleted: [{}]", destFile.uri());
      return true;
    } catch (RuntimeException e) {
      log.error("Error deleting: [{}]", destFile.uri(), e);
      return false;
    }
  }
}
