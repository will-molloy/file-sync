package com.willmolloy.backup.s3;

import static java.util.Objects.requireNonNull;

import com.willmolloy.backup.Backup;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
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

  // TODO every request costs money.
  //  Redundant to list files we just uploaded etc. How to fix that?
  //  Does the client cache for us?
  @Override
  public Stream<Path> scan() {
    log.info("scan({})", bucketName);
    ListObjectsV2Request request = ListObjectsV2Request.builder().bucket(bucketName).build();
    ListObjectsV2Iterable response = s3Client.listObjectsV2Paginator(request);

    return response.stream()
        .flatMap(listObjectsResponse -> listObjectsResponse.contents().stream())
        .map(S3Object::key)
        // TODO do we want to normalise everything to Path? Losing a lot of info.
        //  Redundant to list the objects then make a new request to check size etc.
        .map(Path::of);
  }

  @Override
  public boolean exists(Path relativePath) {
    try {
      head(relativePath.toString());
      return true;
    } catch (NoSuchKeyException e) {
      return false;
    }
  }

  @Override
  public boolean isDirectory(Path relativePath) {
    return false;
  }

  @Override
  public long size(Path relativePath) {
    try {
      HeadObjectResponse response = head(relativePath.toString());
      return response.contentLength();
    } catch (NoSuchKeyException e) {
      return -1;
    }
  }

  @Override
  public long lastModified(Path relativePath) {
    try {
      HeadObjectResponse response = head(relativePath.toString());
      return response.lastModified().toEpochMilli();
    } catch (NoSuchKeyException e) {
      return -1;
    }
  }

  private HeadObjectResponse head(String key) throws NoSuchKeyException {
    HeadObjectRequest request = HeadObjectRequest.builder().bucket(bucketName).key(key).build();
    return s3Client.headObject(request);
  }
}
