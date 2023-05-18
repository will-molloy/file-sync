package com.willmolloy.backup.s3;

import static com.willmolloy.backup.util.PathHelper.ensureUnixSeparator;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;

import com.willmolloy.backup.Backup.Location;
import com.willmolloy.backup.FileTree;
import java.nio.file.Path;
import java.util.TreeMap;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

/**
 * AWS S3 Bucket.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
class S3Bucket implements Location {

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
  public FileTree scan() {
    log.info("Scanning bucket: [{}]", bucketUri());
    ListObjectsV2Request request =
        ListObjectsV2Request.builder()
            .bucket(bucketName)
            .prefix(ensureUnixSeparator(prefix) + "/")
            .build();
    ListObjectsV2Iterable response = s3Client.listObjectsV2Paginator(request);

    Function<S3Object, Path> keyFunc =
        ((Function<S3Object, String>) S3Object::key).andThen(Path::of).andThen(prefix::relativize);

    BinaryOperator<S3File> mergeFunc =
        (first, second) -> {
          log.warn("Scanned duplicate: [{}]", second);
          return second;
        };

    TreeMap<Path, S3File> map =
        response.stream()
            .flatMap(listObjectsResponse -> listObjectsResponse.contents().stream())
            .collect(toMap(keyFunc, S3File::new, mergeFunc, TreeMap::new));
    return FileTree.create(map);
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
}
