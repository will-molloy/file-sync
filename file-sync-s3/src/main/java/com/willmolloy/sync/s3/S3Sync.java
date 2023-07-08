package com.willmolloy.sync.s3;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.willmolloy.sync.util.Md5Helper.md5Base64;
import static com.willmolloy.sync.util.PathHelper.ensureUnixSeparator;
import static com.willmolloy.sync.util.StreamHelper.chunk;

import com.google.common.collect.Lists;
import com.willmolloy.sync.BaseSync;
import com.willmolloy.sync.File;
import com.willmolloy.sync.FileTree;
import com.willmolloy.sync.local.LocalFile;
import com.willmolloy.sync.local.LocalStorage;
import com.willmolloy.sync.statistics.SyncObserver;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
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
 * For sync to AWS S3.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
final class S3Sync extends BaseSync<LocalFile, S3File> {

  private static final Logger log = LogManager.getLogger();

  private final Supplier<S3Client> s3ClientSupplier;
  private final Function<S3Client, S3Waiter> s3WaiterSupplier;
  private final S3Bucket destination;

  S3Sync(
      Supplier<S3Client> s3ClientSupplier,
      Function<S3Client, S3Waiter> s3WaiterSupplier,
      LocalStorage source,
      S3Bucket destination,
      List<SyncObserver> observers) {
    super(source, destination, observers);
    this.s3ClientSupplier = checkNotNull(s3ClientSupplier);
    this.s3WaiterSupplier = checkNotNull(s3WaiterSupplier);
    this.destination = checkNotNull(destination);
  }

  @Override
  protected boolean put(LocalFile sourceFile) {
    Path sourcePath = sourceFile.fullPath();
    String destinationUri = s3Uri(sourceFile);
    try (S3Client s3Client = s3ClientSupplier.get();
        S3Waiter s3Waiter = s3WaiterSupplier.apply(s3Client)) {
      log.debug("Sending put request: [{}] -> [{}]", sourceFile, destinationUri);
      String key = s3Key(sourceFile);
      PutObjectRequest.Builder baseRequest =
          PutObjectRequest.builder()
              .bucket(destination.bucketName())
              .key(key)
              .storageClass(StorageClass.DEEP_ARCHIVE);
      if (sourceFile.isDirectory()) {
        PutObjectRequest request = baseRequest.build();
        s3Client.putObject(request, RequestBody.empty());
      } else {
        // TODO multipart upload for large files
        PutObjectRequest request = baseRequest.contentMD5(md5Base64(sourcePath)).build();
        s3Client.putObject(request, sourcePath);
      }
      wait(key, s3Waiter::waitUntilObjectExists);
      log.debug("Sent put request: [{}] -> [{}]", sourceFile, destinationUri);
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
  protected boolean delete(FileTree<S3File> destSubtree) {
    try (S3Client s3Client = s3ClientSupplier.get();
        S3Waiter s3Waiter = s3WaiterSupplier.apply(s3Client)) {
      log.debug("Sending delete request: [{}]", destSubtree.root().uri());
      if (destSubtree.root().isDirectory()) {
        deleteFolder(s3Client, s3Waiter, destSubtree);
      } else {
        deleteObject(s3Client, s3Waiter, destSubtree.root());
      }
      log.debug("Sent delete request: [{}]", destSubtree.root().uri());
      return true;
    } catch (Exception e) {
      log.error("Error deleting: [{}]", destSubtree.root().uri(), e);
      return false;
    }
  }

  private void deleteFolder(S3Client s3Client, S3Waiter s3Waiter, FileTree<S3File> destSubtree) {
    List<String> keys = destSubtree.leaves().map(this::s3Key).toList();

    for (List<String> chunk : Lists.partition(keys, 1000)) {
      List<ObjectIdentifier> objects =
          chunk.stream().map(key -> ObjectIdentifier.builder().key(key).build()).toList();
      DeleteObjectsRequest request =
          DeleteObjectsRequest.builder()
              .bucket(destination.bucketName())
              .delete(Delete.builder().objects(objects).build())
              .build();
      s3Client.deleteObjects(request);
    }

    for (String key : keys) {
      wait(key, s3Waiter::waitUntilObjectNotExists);
    }
  }

  private void deleteObject(S3Client s3Client, S3Waiter s3Waiter, S3File destFile) {
    String key = s3Key(destFile);
    DeleteObjectRequest request =
        DeleteObjectRequest.builder().bucket(destination.bucketName()).key(key).build();
    s3Client.deleteObject(request);
    wait(key, s3Waiter::waitUntilObjectNotExists);
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

  private void wait(
      String key, java.util.function.Function<HeadObjectRequest, WaiterResponse<?>> waiter) {
    HeadObjectRequest headRequest =
        HeadObjectRequest.builder().bucket(destination.bucketName()).key(key).build();
    // we are supposed to ignore the ResponseOrException here?
    // only populated when successful (even the Exception e.g. 404 for waitUntilObjectNotExists)
    // the method call itself will throw an exception if something went wrong.
    // https://github.com/aws/aws-sdk-java-v2/issues/2460#issuecomment-837136429
    waiter.apply(headRequest);
  }
}
