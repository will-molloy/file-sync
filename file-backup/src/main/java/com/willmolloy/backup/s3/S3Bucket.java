package com.willmolloy.backup.s3;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;

import com.willmolloy.backup.Backup;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.internal.resource.S3BucketResource;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

/**
 * For backups to an AWS S3 Bucket.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public class S3Bucket implements Backup.Destination, Backup.Source {

  private static final Logger log = LogManager.getLogger();

  private final S3Client s3Client;
  private final String bucketName;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public S3Bucket(S3Client s3Client, String bucketName) {
    this.s3Client = requireNonNull(s3Client);
    this.bucketName = requireNonNull(bucketName);
  }

  // TODO expensive to list everything... do we really need this?
  @Override
  public Map<String, Backup.File> scan() {
    log.info("Scanning bucket: {}", bucketName);
    ListObjectsV2Iterable response = s3Client.listObjectsV2Paginator(builder -> builder.bucket(bucketName));
    return response.stream()
        .flatMap(listObjectsResponse -> listObjectsResponse.contents().stream())
        .collect(toMap(S3Object::key, S3ObjectFile::new));
  }

  @Override
  public void put(String key, Path sourceFile) {
    log.info("put({})", key);
    // TODO validate checksum etc.
    s3Client.putObject(builder -> builder.bucket(bucketName).key(key), sourceFile);
  }

  @Override
  public void delete(String key) {
    log.info("delete({})", key);
    s3Client.deleteObject(builder -> builder.bucket(bucketName).key(key));
  }

  private record S3ObjectFile(S3Object s3Object) implements Backup.File {

      @Override
      public long sizeInBytes() {
        return s3Object.size();
      }

      @Override
      public Instant lastModified() {
        return s3Object.lastModified();
      }
    }
}
