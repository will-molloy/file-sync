package com.willmolloy.backup;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * For backups from a File System to another File System (represented by {@link Path}).
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
class FileSystemBackup implements Backup {

  private static final Logger log = LogManager.getLogger();

  private final Path sourceRoot;
  private final Path destRoot;

  FileSystemBackup(Path sourceRoot, Path destRoot) {
    log.info("{}(sourceRoot={}, destRoot={})", getClass().getSimpleName(), sourceRoot, destRoot);
    this.sourceRoot = requireNonNull(sourceRoot);
    this.destRoot = requireNonNull(destRoot);
  }

  @Override
  public Stream<Path> scanSource() {
    return scan(sourceRoot);
  }

  @Override
  public Stream<Path> scanDestination() {
    return scan(destRoot);
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

  @Override
  public void tryCopyOrUpdate(Path path) {
    Path sourcePath = sourceRoot.resolve(path);
    Path destPath = destRoot.resolve(path);

    if (!Files.exists(destPath)) {
      copy(sourcePath, destPath);
    } else {
      // only update files
      if (Files.isRegularFile(sourcePath)) {
        // if the file is a directory on dest, need to delete it first
        if (Files.isDirectory(destPath)) {
          deleteRecursively(destPath);
          copy(sourcePath, destPath);
        } else if (!equals(sourcePath, destPath)) {
          update(sourcePath, destPath);
        }
      }
    }
  }

  private void copy(Path sourcePath, Path destPath) {
    log.info("copy({} -> {})", sourcePath, destPath);
    try {
      Path destParent = destPath.getParent();
      if (destParent != null) {
        Files.createDirectories(destParent);
      }
      Files.copy(sourcePath, destPath, StandardCopyOption.COPY_ATTRIBUTES);
    } catch (IOException e) {
      log.error("Error copying(%s -> %s)".formatted(sourcePath, destPath), e);
    }
  }

  private void update(Path sourcePath, Path destPath) {
    log.info("update({} -> {})", sourcePath, destPath);
    try {
      Files.copy(
          sourcePath,
          destPath,
          StandardCopyOption.COPY_ATTRIBUTES,
          StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      log.error("Error updating(%s -> %s)".formatted(sourcePath, destPath), e);
    }
  }

  private boolean equals(Path sourcePath, Path destPath) {
    log.debug("equals({}, {})", sourcePath, destPath);
    try {
      // this is sufficient? Files.mismatch is quite expensive.
      return Files.size(sourcePath) == Files.size(destPath)
          && Files.getLastModifiedTime(sourcePath).compareTo(Files.getLastModifiedTime(destPath))
              == 0;
    } catch (IOException e) {
      log.error("Error comparing(%s, %s)".formatted(sourcePath, destPath), e);
      return false;
    }
  }

  @Override
  public void tryDelete(Path path) {
    Path sourcePath = sourceRoot.resolve(path);
    Path destPath = destRoot.resolve(path);

    if (!Files.exists(sourcePath)) {
      deleteRecursively(destPath);
    }
  }

  private void deleteRecursively(Path destPath) {
    log.info("delete({})", destPath);
    try {
      if (Files.isDirectory(destPath)) {
        Files.list(destPath).forEach(this::deleteRecursively);
      }
      Files.deleteIfExists(destPath);
    } catch (IOException e) {
      log.error("Error deleting(%s)".formatted(destPath), e);
    }
  }
}
