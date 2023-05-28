package com.willmolloy.backup.s3;

import static com.willmolloy.backup.util.Md5Helper.md5Base64;
import static com.willmolloy.backup.util.PathHelper.ensureUnixSeparator;
import static com.willmolloy.backup.util.StreamHelper.chunk;
import static java.util.Objects.requireNonNull;

import com.willmolloy.backup.BaseBackup;
import com.willmolloy.backup.File;
import com.willmolloy.backup.local.LocalFile;
import com.willmolloy.backup.local.LocalStorage;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.StorageClass;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;

/**
 * For backups to AWS S3.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
final class S3Backup extends BaseBackup<LocalFile, S3File> {

  private static final Logger log = LogManager.getLogger();

  private final S3Client s3Client;
  private final S3Waiter s3Waiter;
  private final S3Bucket destination;

  S3Backup(S3Client s3Client, S3Waiter s3Waiter, LocalStorage source, S3Bucket destination) {
    super(source, destination);
    this.s3Client = requireNonNull(s3Client);
    this.s3Waiter = requireNonNull(s3Waiter);
    this.destination = requireNonNull(destination);
  }

  @Override
  public boolean put(LocalFile sourceFile) {
    Path sourcePath = sourceFile.fullPath();
    String destinationUri = s3Uri(sourceFile);
    try {
      PutObjectRequest.Builder baseRequest =
          PutObjectRequest.builder()
              .bucket(destination.bucketName())
              .key(s3Key(sourceFile))
              .storageClass(StorageClass.DEEP_ARCHIVE);

      if (sourceFile.isDirectory()) {
        PutObjectRequest request = baseRequest.build();
        s3Client.putObject(request, RequestBody.empty());
      } else {
        // TODO multipart upload for large files
        PutObjectRequest request = baseRequest.contentMD5(md5Base64(sourcePath)).build();
        s3Client.putObject(request, sourcePath);
      }

      wait(s3Key(sourceFile), s3Waiter::waitUntilObjectExists);

      log.info("Put: [{}] -> [{}]", sourceFile, destinationUri);
      return true;
    } catch (NoSuchFileException e) {
      log.warn(
          "Skipped put: [{}] -> [{}]. Source file deleted since scan",
          sourcePath,
          destinationUri,
          e);
      return true;
    } catch (Exception e) {
      log.error("Error putting: [{}] -> [{}]", sourcePath, destinationUri, e);
      return false;
    }
  }

  @Override
  public boolean delete(S3File destFile) {
    try {
      if (destFile.isDirectory()) {
        deleteFolder(destFile);
      } else {
        deleteObject(destFile);
      }
      log.info("Deleted: [{}]", destFile.uri());
      return true;
    } catch (NoSuchFileException e) {
      log.debug("Already deleted: [{}]", destFile, e);
      return true;
    } catch (Exception e) {
      log.error("Error deleting: [{}]", destFile.uri(), e);
      return false;
    }
  }

  private void deleteFolder(S3File destFile) throws NoSuchFileException {
    // TODO cache scan
    Stream<S3File> filesToDelete = destination.scan().subtree(destFile).leaves();
    Stream<List<S3File>> chunks = chunk(filesToDelete, 1000);
    chunks
        .map(
            chunk ->
                chunk.stream()
                    .map(s3File -> ObjectIdentifier.builder().key(s3Key(s3File)).build())
                    .toList())
        .map(
            objects ->
                DeleteObjectsRequest.builder()
                    .bucket(destination.bucketName())
                    .delete(Delete.builder().objects(objects).build())
                    .build())
        .forEach(s3Client::deleteObjects);
  }

  private void deleteObject(S3File destFile) {
    DeleteObjectRequest request =
        DeleteObjectRequest.builder().bucket(destination.bucketName()).key(s3Key(destFile)).build();
    s3Client.deleteObject(request);
    wait(s3Key(destFile), s3Waiter::waitUntilObjectNotExists);
  }

  private String s3Uri(File file) {
    return file.isDirectory()
        ? destination.folderUri(file.relativePath())
        : destination.objectUri(file.relativePath());
  }

  private String s3Key(File file) {
    String key = ensureUnixSeparator(destination.prefix().resolve(file.relativePath()));
    return file.isDirectory() ? key + "/" : key;
  }

  // TODO this just throws errors - not using it right?
  private void wait(String key, Function<HeadObjectRequest, WaiterResponse<?>> waiter) {
    HeadObjectRequest headRequest =
        HeadObjectRequest.builder().bucket(destination.bucketName()).key(key).build();
    // we are supposed to ignore the ResponseOrException here?
    // only populated when successful (even the Exception e.g. 404 for waitUntilObjectNotExists)
    // the method call itself will throw an exception if something went wrong.
    // https://github.com/aws/aws-sdk-java-v2/issues/2460#issuecomment-837136429
    waiter.apply(headRequest);
  }
}
