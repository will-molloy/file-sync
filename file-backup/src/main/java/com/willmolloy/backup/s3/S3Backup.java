package com.willmolloy.backup.s3;

import static com.willmolloy.backup.util.Md5Helper.md5AsBase64;
import static java.util.Objects.requireNonNull;

import com.willmolloy.backup.Backup;
import com.willmolloy.backup.local.LocalStorage;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
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

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  private final S3Client s3Client;

  private final LocalStorage source;
  private final S3Bucket destination;

  public S3Backup(S3Client s3Client, LocalStorage source, S3Bucket destination) {
    this.s3Client = requireNonNull(s3Client);
    this.source = requireNonNull(source);
    this.destination = requireNonNull(destination);
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
  public boolean put(String key) {
    Path sourcePath = source.root().resolve(key);
    String destinationUri = destination.objectUri(key);
    log.info("Putting: [{}] -> [{}]", sourcePath, destinationUri);
    try {
      PutObjectRequest request =
          PutObjectRequest.builder()
              .bucket(destination.bucketName())
              .key(destination.prefix() + key)
              .contentMD5(md5AsBase64(sourcePath))
              .build();
      s3Client.putObject(request, sourcePath);
      return true;
    } catch (RuntimeException | IOException e) {
      log.error("Error putting: [%s] -> [%s]".formatted(sourcePath, destinationUri), e);
      return false;
    }
  }

  @Override
  public boolean delete(String key) {
    String destinationUri = destination.objectUri(key);
    log.info("Deleting: [{}]", destinationUri);
    try {
      DeleteObjectRequest request =
          DeleteObjectRequest.builder()
              .bucket(destination.bucketName())
              .key(destination.prefix() + key)
              .build();
      s3Client.deleteObject(request);
      return true;
    } catch (RuntimeException e) {
      log.error("Error deleting: [%s]".formatted(destinationUri), e);
      return false;
    }
  }

  @Override
  public String toString() {
    return "S3Backup[source=%s, destination=%s]".formatted(source, destination);
  }
}
