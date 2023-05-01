package com.willmolloy.backup.filesystem;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;

import com.willmolloy.backup.Backup;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Map;
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
  public Map<String, Backup.File> scan() {
    log.info("Scanning directory: {}", root);
    return walk(root)
        // skip self
        .skip(1)
        // strip prefix so can compare source & dest paths
        .collect(toMap(path -> root.relativize(path).toString(), PathFile::new));
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
      return Stream.of(path);
    }
  }

  @Override
  public void put(String key, Path sourceFile) {
    log.info("put({})", key);
    Path destPath = root.resolve(key);
    try {
      Path destParent = destPath.getParent();
      if (destParent != null) {
        Files.createDirectories(destParent);
      }
      Files.copy(
          sourceFile,
          destPath,
          StandardCopyOption.COPY_ATTRIBUTES,
          StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e){
      log.error("Error putting(%s -> %s)".formatted(sourceFile, destPath), e);
    }
  }

  @Override
  public void delete(String key) {
    log.info("delete({})", key);
    Path destPath = root.resolve(key);
    deleteRecursively(destPath);
  }

  private void deleteRecursively(Path destPath) {
    try {
      if (Files.isDirectory(destPath)) {
        Files.list(destPath).forEach(this::deleteRecursively);
      }
      Files.deleteIfExists(destPath);
    } catch (IOException e) {
      log.error("Error deleting(%s)".formatted(destPath), e);
    }
  }

  @Override
  public Path get(String key) {
    return root.resolve(key);
  }

  private record PathFile(Path path) implements Backup.File {

      @Override
      public long sizeInBytes() {
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
}
