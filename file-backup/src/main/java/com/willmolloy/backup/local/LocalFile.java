package com.willmolloy.backup.filesystem;

import static java.util.Objects.requireNonNull;

import com.willmolloy.backup.Backup;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Local file on disk.
 *
 * @param path path to the file
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
record LocalFile(Path path) implements Backup.File {

  private static final Logger log = LogManager.getLogger();

  LocalFile {
    requireNonNull(path);
  }

  @Override
  public long size() {
    try {
      return Files.size(path);
    } catch (IOException e) {
      log.error("Error getting size of file: [{}]", path);
      return -1;
    }
  }

  @Override
  public Instant lastModified() {
    try {
      return Files.getLastModifiedTime(path).toInstant();
    } catch (IOException e) {
      log.error("Error getting last modified time of file: [{}]", path);
      return Instant.MIN;
    }
  }
}
