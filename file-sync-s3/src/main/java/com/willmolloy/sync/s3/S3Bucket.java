package com.willmolloy.sync.s3;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.willmolloy.sync.util.PathHelper.ensureUnixSeparator;

import com.willmolloy.sync.FileTree;
import com.willmolloy.sync.Location;
import java.nio.file.Path;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

/**
 * AWS S3 Bucket.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
final class S3Bucket implements Location<S3File> {

  private static final Logger log = LogManager.getLogger();

  private static final String S3_BASE_URI = "https://s3.console.aws.amazon.com/s3/";

  private final Supplier<S3Client> s3ClientSupplier;

  private final String bucketName;
  private final Path prefix;

  S3Bucket(Supplier<S3Client> s3ClientSupplier, String bucketName, Path prefix) {
    this.s3ClientSupplier = checkNotNull(s3ClientSupplier);
    this.bucketName = checkNotNull(bucketName);
    this.prefix = checkNotNull(prefix);
  }

  @Override
  public FileTree<S3File> scan() {
    try (S3Client s3Client = s3ClientSupplier.get()) {

      ListObjectsV2Request request =
          ListObjectsV2Request.builder()
              .bucket(bucketName)
              .prefix(ensureUnixSeparator(prefix) + "/")
              .build();
      ListObjectsV2Iterable paginatedResponse = s3Client.listObjectsV2Paginator(request);

      FileTree.Builder<S3File> builder =
          FileTree.builder(
              S3File.directoryFiller(this, ""), path -> S3File.directoryFiller(this, path));
      for (ListObjectsV2Response response : paginatedResponse) {
        for (S3Object s3Object : response.contents()) {
          S3File file = S3File.fromS3Object(this, s3Object);
          builder.insert(file);
        }
      }
      return builder.build();
    }
  }

  String bucketName() {
    return bucketName;
  }

  Path prefix() {
    return prefix;
  }

  @Override
  public String toString() {
    return "%s[%s]".formatted(getClass().getSimpleName(), bucketUri());
  }

  private String bucketUri() {
    return S3_BASE_URI + "buckets/%s?prefix=%s/".formatted(bucketName, ensureUnixSeparator(prefix));
  }

  String objectUri(Path key) {
    return S3_BASE_URI
        + "object/%s?prefix=%s".formatted(bucketName, ensureUnixSeparator(prefix.resolve(key)));
  }

  String folderUri(Path key) {
    return S3_BASE_URI
        + "buckets/%s?prefix=%s/".formatted(bucketName, ensureUnixSeparator(prefix.resolve(key)));
  }
}
