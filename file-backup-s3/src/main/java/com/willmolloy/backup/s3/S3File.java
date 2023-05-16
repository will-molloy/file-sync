package com.willmolloy.backup.s3;

import static java.util.Objects.requireNonNull;

import com.willmolloy.backup.FileTree;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * A file in AWS S3.
 *
 * @param s3Object the underlying S3 object
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
record S3File(S3Object s3Object) implements FileTree.File {

  private static final Logger log = LogManager.getLogger();

  S3File {
    requireNonNull(s3Object);
  }

  @Override
  public long size() {
    try {
      return s3Object.size();
    } catch (RuntimeException e) {
      log.error("Error getting size of object: [%s]".formatted(s3Object), e);
      return 0;
    }
  }

  @Override
  public boolean isDirectory() {
    return false;
  }
}
