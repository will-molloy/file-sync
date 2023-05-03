package com.willmolloy.backup.s3;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;

import com.willmolloy.backup.Backup.File;
import com.willmolloy.backup.Backup.Location;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
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
public class S3Bucket implements Location {

  private static final Logger log = LogManager.getLogger();

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  private final S3Client s3Client;

  private final String bucketName;
  private final String prefix;

  public S3Bucket(S3Client s3Client, String bucketName, String prefix) {
    this.s3Client = requireNonNull(s3Client);
    this.bucketName = requireNonNull(bucketName);
    this.prefix = requireNonNull(prefix);
  }

  @Override
  public Map<String, File> scan() {
    log.info("Scanning bucket: {}", bucketUri());
    ListObjectsV2Request request =
        ListObjectsV2Request.builder().bucket(bucketName).prefix(prefix).build();
    ListObjectsV2Iterable response = s3Client.listObjectsV2Paginator(request);

    return response.stream()
        .flatMap(listObjectsResponse -> listObjectsResponse.contents().stream())
        .collect(toMap(S3Object::key, S3File::new));
  }

  public String bucketName() {
    return bucketName;
  }

  public String prefix() {
    return prefix;
  }

  @Override
  public String toString() {
    return "S3Bucket[%s]".formatted(bucketUri());
  }

  private String bucketUri() {
    return "https://s3.console.aws.amazon.com/s3/buckets/%s".formatted(bucketName);
  }

  public String objectUri(String key) {
    return "https://s3.console.aws.amazon.com/s3/object/%s?prefix=%s%s"
        .formatted(bucketName, prefix, key);
  }
}
