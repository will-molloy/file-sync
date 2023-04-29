package com.willmolloy.backup;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
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
  public FileTree scanSource() {
    return scan(sourceRoot);
  }

  @Override
  public FileTree scanDestination() {
    return scan(destRoot);
  }

  private static FileTree scan(Path root) {
    log.debug("scan({})", root);
    return scan(root, root);
  }

  private static FileTree scan(Path path, Path root) {
    if (Files.isRegularFile(path)) {
      return new FileTree();
    }

    // bfs
    try {
      Map<Path, FileTree.Node> map =
          Files.list(path)
              // avoid AccessDeniedException
              .filter(Files::isReadable)
              .collect(
                  toMap(
                      // strip prefix so can compare source & destination
                      child -> root.relativize(child),
                      child -> scan(child, root).root()));
      return new FileTree(map);
    } catch (IOException e) {
      log.error("Error listing directory [%s]".formatted(path), e);
      throw new UncheckedIOException(e);
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
