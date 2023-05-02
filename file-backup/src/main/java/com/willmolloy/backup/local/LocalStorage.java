package com.willmolloy.backup.local;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;

import com.willmolloy.backup.Backup;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Represents a local storage location.
 *
 * @param root root directory
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public record LocalStorage(Path root) implements Backup.Location {

  private static final Logger log = LogManager.getLogger();

  public LocalStorage {
    requireNonNull(root);
  }

  @Override
  public Map<String, Backup.File> scan() {
    log.info("Scanning directory: {}", root);
    return walk(root)
        .parallel()
        // strip prefix so can compare source & dest paths
        .collect(toMap(path -> root.relativize(path).toString(), LocalFile::new));
  }

  private Stream<Path> walk(Path path) {
    log.debug("walk({})", path);
    // avoid AccessDeniedException
    if (!Files.isReadable(path)) {
      return Stream.of();
    }

    if (Files.isDirectory(path)) {
      try {
        return Files.list(path).flatMap(this::walk);
      } catch (IOException e) {
        log.error("Error listing directory [%s]".formatted(path), e);
        return Stream.of();
      }
    } else {
      // limit to files only for now
      // TODO what about empty dirs?
      // TODO what about overwriting dirs with files?
      //  Could be files without extensions that cause this?
      return Stream.of(path);
    }
  }
}
