package com.willmolloy.backup.filesystem;

import static java.util.Objects.requireNonNull;

import com.willmolloy.backup.Backup;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicLong;
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

  private final Backup.Location source;
  private final Backup.Location destination;

  public FileSystemBackup(Location source, Location destination) {
    this.source = requireNonNull(source);
    this.destination = requireNonNull(destination);
  }

  @Override
  public Location source() {
    return source;
  }

  @Override
  public Location destination() {
    return destination;
  }

  @Override
  public void copy(Path relativePath) {
    Path sourcePath = source.root().resolve(relativePath);
    Path destPath = destination.root().resolve(relativePath);
    log.info("copy({} -> {})", sourcePath, destPath);
    copyCount.incrementAndGet();
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
    updateCount.incrementAndGet();
    try {
      copyOrUpdate(sourcePath, destPath);
    } catch (IOException e) {
      log.error("Error updating(%s -> %s)".formatted(sourcePath, destPath), e);
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
  public void delete(Path relativePath) {
    Path destPath = destination.root().resolve(relativePath);
    log.info("delete({})", destPath);
    deleteCount.incrementAndGet();
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
  public Statistics statistics() {
    return new Statistics(copyCount.get(), updateCount.get(), deleteCount.get());
  }

  @Override
  public String toString() {
    return "%s[source=%s, destination=%s]"
        .formatted(getClass().getSimpleName(), source, destination);
  }
}
