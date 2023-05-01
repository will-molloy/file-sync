package com.willmolloy.backup.filesystem;

import static java.util.Objects.requireNonNull;

import com.willmolloy.backup.Backup;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicLong;

import com.willmolloy.backup.Backup.Location;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * For backups to/from a File System (represented by {@link Path}).
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public class FileSystemBackup implements Backup {

  private static final Logger log = LogManager.getLogger();

  private final AtomicLong copyCount = new AtomicLong();
  private final AtomicLong updateCount = new AtomicLong();
  private final AtomicLong deleteCount = new AtomicLong();

  private final FileSystem source;
  private final FileSystem destination;

  public FileSystemBackup(FileSystem source, FileSystem destination) {
    this.source = requireNonNull(source);
    this.destination = requireNonNull(destination);
  }

  @Override
  public FileSystem source() {
    return source;
  }

  @Override
  public FileSystem destination() {
    return destination;
  }

  @Override
  public void copy(Path sourceFile) {
    log.info("copy({} -> {})", sourceFile, destPath);
    copyCount.incrementAndGet();
    try {
      copyOrUpdate(sourceFile, destPath);
    } catch (IOException e) {
      log.error("Error copying(%s -> %s)".formatted(sourceFile, destPath), e);
    }
  }

  @Override
  public void update(Path sourceFile, Path destFile) {
    updateCount.incrementAndGet();
    try {
      copyOrUpdate(sourceFile, destFile);
    } catch (IOException e) {
      log.error("Error updating(%s -> %s)".formatted(sourceFile, destFile), e);
    }
  }

  @Override
  public void delete(Path destFile) {
    log.info("delete({})", destFile);
    deleteCount.incrementAndGet();
    deleteRecursively(destFile);
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

  private void copyOrUpdate(Path sourcePath, Path destPath) throws IOException {
    Path destParent = destPath.getParent();
    if (destParent != null && Files.exists(sourcePath)) {
      Files.createDirectories(destParent);
    }
    Files.copy(
        sourcePath,
        destPath,
        StandardCopyOption.COPY_ATTRIBUTES,
        StandardCopyOption.REPLACE_EXISTING);
  }

  @Override
  public Statistics statistics() {
    return new Statistics(copyCount.get(), updateCount.get(), deleteCount.get());
  }

  @Override
  public String toString() {
    return "%s[source=%s, destination=%s]"
        .formatted(getClass().getSimpleName(), source, destination);
  }
}
