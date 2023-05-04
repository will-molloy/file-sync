package com.willmolloy.backup.local;

import static com.willmolloy.backup.util.Preconditions.require;
import static java.util.Objects.requireNonNull;

import com.willmolloy.backup.Backup.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;
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
  public OptionalLong size() {
    try {
      return OptionalLong.of(Files.size(path));
    } catch (IOException e) {
      log.error("Error getting size of file: [%s]".formatted(path), e);
      return OptionalLong.empty();
    }
  }

  @Override
  public Optional<Instant> lastModified() {
    try {
      return Optional.of(Files.getLastModifiedTime(path).toInstant());
    } catch (IOException e) {
      log.error("Error getting last modified time of file: [%s]".formatted(path), e);
      return Optional.empty();
    }
  }
}
