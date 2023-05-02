package com.willmolloy.backup.s3;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;

import com.willmolloy.backup.Backup;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.file.Path;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

/**
 * For backups to an AWS S3 Bucket.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public class S3Bucket implements Backup.Location {

  private static final Logger log = LogManager.getLogger();

  private final S3Client s3Client;
  private final String bucketName;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public S3Bucket(S3Client s3Client, String bucketName) {
    this.s3Client = requireNonNull(s3Client);
    this.bucketName = requireNonNull(bucketName);
  }

  @Override
  public Path root() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Map<String, Backup.File> scan() {
    log.info("scan({})", bucketName);
    ListObjectsV2Request request = ListObjectsV2Request.builder().bucket(bucketName).build();
    ListObjectsV2Iterable response = s3Client.listObjectsV2Paginator(request);

    return response.stream()
        .flatMap(listObjectsResponse -> listObjectsResponse.contents().stream())
        .collect(toMap(S3Object::key, S3ObjectFile::new));
  }
}
