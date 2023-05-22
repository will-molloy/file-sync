package com.willmolloy.backup.s3;

import static com.willmolloy.backup.util.PathHelper.ensureUnixSeparator;
import static java.util.Objects.requireNonNull;

import com.willmolloy.backup.FileTree;
import com.willmolloy.backup.Location;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
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
class S3Bucket implements Location<S3File> {

  private static final Logger log = LogManager.getLogger();

  private static final String S3_BASE_URI = "https://s3.console.aws.amazon.com/s3/";

  private final S3Client s3Client;

  private final String bucketName;
  private final Path prefix;

  S3Bucket(S3Client s3Client, String bucketName, Path prefix) {
    this.s3Client = requireNonNull(s3Client);
    this.bucketName = requireNonNull(bucketName);
    this.prefix = requireNonNull(prefix);
  }

  @Override
  public FileTree<S3File> scan() {
    log.info("Scanning bucket: [{}]", bucketUri());
    ListObjectsV2Request request =
        ListObjectsV2Request.builder()
            .bucket(bucketName)
            .prefix(ensureUnixSeparator(prefix) + "/")
            .build();
    ListObjectsV2Iterable paginatedResponse = s3Client.listObjectsV2Paginator(request);

    Set<S3File> set = new HashSet<>();
    for (ListObjectsV2Response response : paginatedResponse) {
      for (S3Object s3Object : response.contents()) {
        S3File file = new S3File(this, s3Object);
        if (!set.add(file)) {
          log.warn("Scanned duplicate: [{}]", file);
        }
      }
    }
    return FileTree.from(set);
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
