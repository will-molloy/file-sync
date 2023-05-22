package com.willmolloy.backup.s3;

import static com.willmolloy.backup.util.Preconditions.require;
import static java.util.Objects.requireNonNull;

import com.willmolloy.backup.BaseFile;
import com.willmolloy.backup.File;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Optional;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * A file in AWS S3.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
final class S3File extends BaseFile implements File {

  private final S3Bucket s3Bucket;
  private final Path relativePath;
  private final long size;
  private final boolean isDirectory;

  S3File(S3Bucket s3Bucket, S3Object s3Object) {
    FileSystem fs = s3Bucket.prefix().getFileSystem();
    Path path = fs.getPath(s3Object.key());
    require(
        path.startsWith(s3Bucket.prefix()),
        "Requires object key [%s] to be under bucket prefix [%s]"
            .formatted(path, s3Bucket.prefix()));
    this.s3Bucket = requireNonNull(s3Bucket);
    this.relativePath = s3Bucket.prefix().relativize(path);
    this.size = Optional.ofNullable(s3Object.size()).orElse(0L);
    this.isDirectory = s3Object.key().endsWith("/");
  }

  @Override
  public String uri() {
    return isDirectory ? s3Bucket.folderUri(relativePath) : s3Bucket.objectUri(relativePath);
  }

  @Override
  public Path relativePath() {
    return relativePath;
  }

  @Override
  public long size() {
    return size;
  }

  @Override
  public boolean isDirectory() {
    return isDirectory;
  }
}
