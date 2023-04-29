package com.willmolloy.backup.filesystem;

import static java.util.Objects.requireNonNull;

import com.willmolloy.backup.Backup;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * For backups to/from a File System (represented by {@link Path}).
 *
 * @param root root directory
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public record FileSystem(Path root) implements Backup.Source, Backup.Destination {

  private static final Logger log = LogManager.getLogger();

  public FileSystem {
    requireNonNull(root);
  }

  @Override
  public Stream<Path> scan() {
    return scan(root);
  }

  private Stream<Path> scan(Path root) {
    log.info("scan({})", root);
    return walk(root)
        // skip self
        .skip(1)
        // strip prefix so can compare source & dest paths
        .map(root::relativize);
  }

  private Stream<Path> walk(Path path) {
    log.debug("walk({})", path);
    // avoid AccessDeniedException
    if (!Files.isReadable(path)) {
      return Stream.of();
    }

    if (Files.isDirectory(path)) {
      try {
        return Stream.concat(Stream.of(path), Files.list(path).flatMap(this::walk));
      } catch (IOException e) {
        log.error("Error listing directory [%s]".formatted(path), e);
        return Stream.of();
      }
    } else {
      return Stream.of(path);
    }
  }
}
