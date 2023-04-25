package com.willmolloy;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.UncheckedIOException;
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
public class FileSystemToFileSystemJob implements Job {

  private static final Logger log = LogManager.getLogger();

  private final Path sourceRoot;
  private final Path destRoot;

  public FileSystemToFileSystemJob(Path sourceRoot, Path destRoot) {
    log.debug("FileSystemToFileSystemJob(sourceRoot={}, destRoot={})", sourceRoot, destRoot);
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

  // avoid AccessDeniedException
  private Stream<Path> walkReadable(Path path) {
    if (!Files.isReadable(path)) {
      return Stream.of();
    }

    if (Files.isDirectory(path)) {
      try {
        return Stream.concat(Stream.of(path), Files.list(path).flatMap(this::walkReadable));
      } catch (IOException e) {
        log.error("Error listing directory [%s]".formatted(path), e);
        throw new UncheckedIOException(e);
      }
    } else {
      return Stream.of(path);
    }
  }

  @Override
  public void copy(Path file) {
    log.debug("copy({})", file);
    Path sourceFile = sourceRoot.resolve(file);
    Path destinationFile = destRoot.resolve(file);
    try {
      Path destinationParent = destinationFile.getParent();
      if (destinationParent != null) {
        Files.createDirectories(destinationParent);
      }
      Files.copy(sourceFile, destinationFile, StandardCopyOption.COPY_ATTRIBUTES);
    } catch (IOException e) {
      log.error("Error copying file [%s] to destination".formatted(file), e);
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void delete(Path file) {
    log.debug("delete({})", file);
    Path destinationFile = destRoot.resolve(file);
    try {
      deleteRecursively(destinationFile);
    } catch (IOException e) {
      log.error("Error deleting file [%s] from destination".formatted(file), e);
      throw new UncheckedIOException(e);
    }
  }

  private void deleteRecursively(Path path) throws IOException {
    if (Files.isDirectory(path)) {
      for (Path child : Files.list(path).toList()) {
        deleteRecursively(child);
      }
    }
    Files.deleteIfExists(path);
  }

  @Override
  public void update(Path file) {
    log.debug("update({})", file);
    Path sourceFile = sourceRoot.resolve(file);
    Path destinationFile = destRoot.resolve(file);
    if (equals(sourceFile, destinationFile)) {
      return;
    }
    try {
      Files.copy(
          sourceFile,
          destinationFile,
          StandardCopyOption.REPLACE_EXISTING,
          StandardCopyOption.COPY_ATTRIBUTES);
    } catch (IOException e) {
      log.error("Error updating file [%s] on destination".formatted(file), e);
      throw new UncheckedIOException(e);
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
      log.error("Error comparing files [%s, %s]".formatted(sourcePath, destinationPath), e);
      throw new UncheckedIOException(e);
    }
  }
}
