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
  public void copy(Path relativePath) {
    Path sourcePath = source.root().resolve(relativePath);
    Path destPath = destination.root().resolve(relativePath);
    log.info("copy({} -> {})", sourcePath, destPath);
    try {
      copyOrUpdate(sourcePath, destPath);
    } catch (IOException e) {
      log.error("Error copying(%s -> %s)".formatted(sourcePath, destPath), e);
    }
  }

  @Override
  public void update(Path relativePath) {
    Path sourcePath = source.root().resolve(relativePath);
    Path destPath = destination.root().resolve(relativePath);
    log.info("update({} -> {})", sourcePath, destPath);
    try {
      copyOrUpdate(sourcePath, destPath);
    } catch (IOException e) {
      log.error("Error updating(%s -> %s)".formatted(sourcePath, destPath), e);
    }
  }

  private void copyOrUpdate(Path sourcePath, Path destPath) throws IOException {
    Path destParent = destPath.getParent();
    if (destParent != null && Files.exists(sourcePath)){
      Files.createDirectories(destParent);
    }
    Files.copy(
        sourcePath,
        destPath,
        StandardCopyOption.COPY_ATTRIBUTES,
        StandardCopyOption.REPLACE_EXISTING);
  }

  @Override
  public void delete(Path relativePath) {
    Path destPath = destination.root().resolve(relativePath);
    log.info("delete({})", destPath);
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
}
