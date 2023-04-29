package com.willmolloy.backup.filesystem;

import static java.util.Objects.requireNonNull;

import com.willmolloy.backup.Backup;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * For backups to/from a File System (represented by {@link Path}).
 *
 * @param source source file system
 * @param destination destination file system
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public record FileSystemBackup(FileSystem source, FileSystem destination) implements Backup {

  private static final Logger log = LogManager.getLogger();

  public FileSystemBackup {
    requireNonNull(source);
    requireNonNull(destination);
  }

  @Override
  public void tryCopyOrUpdate(Path path) {
    Path sourcePath = source.root().resolve(path);
    Path destPath = destination.root().resolve(path);

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
    Path sourcePath = source.root().resolve(path);
    Path destPath = destination.root().resolve(path);

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
