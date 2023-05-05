package com.willmolloy.backup.local;

import static com.willmolloy.backup.util.Md5Helper.md5Base16;
import static com.willmolloy.backup.util.Preconditions.require;
import static java.util.Objects.requireNonNull;

import com.willmolloy.backup.Backup.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Local file on disk.
 *
 * @param path path to the file
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
record LocalFile(Path path) implements File {

  private static final Logger log = LogManager.getLogger();

  LocalFile {
    requireNonNull(path);
    require(Files.isRegularFile(path), "Requires a file: [%s]".formatted(path));
  }

  @Override
  public long size() {
    try {
      return Files.size(path);
    } catch (IOException e) {
      log.error("Error getting size of file: [%s]".formatted(path), e);
      return 0;
    }
  }

  @Override
  public String etag() {
    try {
      return "\"%s\"".formatted(md5Base16(path));
    } catch (IOException e) {
      log.error("Error computing MD5 Digest of file: [%s]".formatted(path), e);
      return "";
    }
  }
}
