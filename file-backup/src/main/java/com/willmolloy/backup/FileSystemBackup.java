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
    log.debug("FileSystemBackup(sourceRoot={}, destRoot={})", sourceRoot, destRoot);
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
    log.debug("scan({})", root);
    return walkReadable(root)
        // skip self
        .skip(1)
        // strip prefix so can compare source & destination
        .map(root::relativize);
  }

  private Stream<Path> walkReadable(Path path) {
    // avoid AccessDeniedException
    if (!Files.isReadable(path)) {
      return Stream.of();
    }

    if (Files.isDirectory(path)) {
      try {
        return Stream.concat(Stream.of(path), Files.list(path).flatMap(this::walkReadable));
      } catch (IOException e) {
        log.error("Error listing directory [%s]".formatted(path), e);
        return Stream.of();
      }
    } else {
      return Stream.of(path);
    }
  }

  @Override
  public void copyOrUpdate(Path file) {
    Path sourceFile = sourceRoot.resolve(file);
    Path destinationFile = destRoot.resolve(file);

    if (!Files.exists(destinationFile)) {
      copy(sourceFile, destinationFile);
    } else if (!equals(sourceFile, destinationFile)) {
      update(sourceFile, destinationFile);
    }
  }

  private void copy(Path sourceFile, Path destinationFile) {
    log.debug("copy({} -> {})", sourceFile, destinationFile);
    try {
      Path destinationParent = destinationFile.getParent();
      if (destinationParent != null) {
        Files.createDirectories(destinationParent);
      }
      Files.copy(sourceFile, destinationFile, StandardCopyOption.COPY_ATTRIBUTES);
    } catch (IOException e) {
      log.error("Error copying(%s -> %s)".formatted(sourceFile, destinationFile), e);
    }
  }

  private void update(Path sourceFile, Path destinationFile) {
    log.debug("update({} -> {})", sourceFile, destinationFile);
    try {
      Files.copy(
          sourceFile,
          destinationFile,
          StandardCopyOption.COPY_ATTRIBUTES,
          StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      log.error("Error updating(%s -> %s)".formatted(sourceFile, destinationFile), e);
    }
  }

  private boolean equals(Path sourcePath, Path destinationPath) {
    log.debug("equals({}, {})", sourcePath, destinationPath);
    try {
      // this is sufficient? Files.mismatch is quite expensive.
      return Files.size(sourcePath) == Files.size(destinationPath)
          && Files.getLastModifiedTime(sourcePath)
                  .compareTo(Files.getLastModifiedTime(destinationPath))
              == 0;
    } catch (IOException e) {
      log.error("Error comparing(%s, %s)".formatted(sourcePath, destinationPath), e);
      return false;
    }
  }

  @Override
  public void delete(Path file) {
    Path sourceFile = sourceRoot.resolve(file);
    Path destinationFile = destRoot.resolve(file);

    if (!Files.exists(sourceFile)) {
      deleteRecursively(destinationFile);
    }
  }

  private void deleteRecursively(Path destinationFile) {
    log.debug("delete({})", destinationFile);
    try {
      if (Files.isDirectory(destinationFile)) {
        Files.list(destinationFile).forEach(this::deleteRecursively);
      }
      Files.delete(destinationFile);
    } catch (IOException e) {
      log.error("Error deleting(%s)".formatted(destinationFile), e);
    }
  }
}
