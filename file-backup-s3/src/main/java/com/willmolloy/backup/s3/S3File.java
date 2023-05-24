package com.willmolloy.backup.s3;

import static com.willmolloy.backup.util.Preconditions.require;
import static java.util.Objects.requireNonNull;

import com.willmolloy.backup.BaseFile;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Optional;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * A file in AWS S3.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
final class S3File extends BaseFile {

  static S3File fromS3Object(S3Bucket s3Bucket, S3Object s3Object) {
    FileSystem fs = s3Bucket.prefix().getFileSystem();
    Path path = fs.getPath(s3Object.key());
    require(
        path.startsWith(s3Bucket.prefix()),
        "Requires object key [%s] to be under bucket prefix [%s]"
            .formatted(path, s3Bucket.prefix()));
    return new S3File(
        s3Bucket,
        s3Bucket.prefix().relativize(path),
        s3Object.key().endsWith("/"),
        Optional.ofNullable(s3Object.size()).orElse(0L));
  }

  static S3File directoryFiller(S3Bucket s3Bucket, String relativePath) {
    FileSystem fs = s3Bucket.prefix().getFileSystem();
    return new S3File(s3Bucket, fs.getPath(relativePath), true, 0);
  }

  private final S3Bucket s3Bucket;
  private final Path relativePath;
  private final boolean isDirectory;

  private S3File(S3Bucket s3Bucket, Path relativePath, boolean isDirectory, long size) {
    super(relativePath, isDirectory, size);
    this.s3Bucket = requireNonNull(s3Bucket);
    this.relativePath = requireNonNull(relativePath);
    this.isDirectory = isDirectory;
  }

  @Override
  public String uri() {
    return isDirectory ? s3Bucket.folderUri(relativePath) : s3Bucket.objectUri(relativePath);
  }
}
