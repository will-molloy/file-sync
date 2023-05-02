package com.willmolloy.backup.s3;

import static java.util.Objects.requireNonNull;

import com.willmolloy.backup.Backup;
import java.time.Instant;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * A file in S3.
 *
 * @param s3Object the underlying S3 object
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
record S3ObjectFile(S3Object s3Object) implements Backup.File {

  S3ObjectFile {
    requireNonNull(s3Object);
  }

  @Override
  public long size() {
    return s3Object.size();
  }

  @Override
  public Instant lastModified() {
    return s3Object.lastModified();
  }
}
