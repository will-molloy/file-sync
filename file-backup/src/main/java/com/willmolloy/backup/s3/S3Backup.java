package com.willmolloy.backup.s3;

import static java.util.Objects.requireNonNull;

import com.willmolloy.backup.Backup;
import com.willmolloy.backup.local.LocalStorage;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.file.Path;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * For backups to AWS S3.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public class S3Backup implements Backup<LocalStorage, S3Bucket> {

  private static final Logger log = LogManager.getLogger();

  private final LocalStorage source;
  private final S3Bucket destination;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  private final S3Client s3Client;

  public S3Backup(LocalStorage source, S3Bucket destination, S3Client s3Client) {
    this.source = requireNonNull(source);
    this.destination = requireNonNull(destination);
    this.s3Client = requireNonNull(s3Client);
  }

  @Override
  public LocalStorage source() {
    return source;
  }

  @Override
  public S3Bucket destination() {
    return destination;
  }

  @Override
  public void copy(String key) {
    put(key);
  }

  @Override
  public void update(String key) {
    put(key);
  }

  private void put(String key) {
    Path sourcePath = source.root().resolve(key);
    String destinationUri = destination.objectUri(key);
    log.info("put({} -> {})", sourcePath, destinationUri);
    try {
      // TODO set MD5
      PutObjectRequest request =
          PutObjectRequest.builder()
              .bucket(destination.bucketName())
              .key(destination.prefix() + key)
              .build();
      s3Client.putObject(request, sourcePath);
    } catch (RuntimeException e) {
      log.error("Error putting({} -> {})", sourcePath, destinationUri);
    }
  }

  @Override
  public void delete(String key) {
    String destinationUri = destination.objectUri(key);
    log.info("delete({})", destinationUri);
    try {
      DeleteObjectRequest request =
          DeleteObjectRequest.builder()
              .bucket(destination.bucketName())
              .key(destination.prefix() + key)
              .build();
      s3Client.deleteObject(request);
    } catch (RuntimeException e) {
      log.error("Error deleting({})", destinationUri);
    }
  }

  @Override
  public String toString() {
    return "S3Backup[source=%s, destination=%s]".formatted(source, destination);
  }
}
