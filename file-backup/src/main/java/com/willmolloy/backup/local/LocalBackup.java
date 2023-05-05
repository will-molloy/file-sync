package com.willmolloy.backup.local;

import static java.util.Objects.requireNonNull;

import com.willmolloy.backup.Backup;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * For backups to/from locally mounted storage. Either local disk, or mounted NAS, etc.
 *
 * @param source source
 * @param destination destination
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public record LocalBackup(LocalStorage source, LocalStorage destination)
    implements Backup<LocalStorage, LocalStorage> {

  private static final Logger log = LogManager.getLogger();

  public LocalBackup(LocalStorage source, LocalStorage destination) {
    this.source = requireNonNull(source);
    this.destination = requireNonNull(destination);
  }

  @Override
  public boolean put(String key) {
    Path sourcePath = source.root().resolve(key);
    Path destPath = destination.root().resolve(key);

    if (!Files.exists(destPath)) {
      log.info("Copying: [{}] -> [{}]", sourcePath, destPath);
    } else {
      log.info("Updating: [{}] -> [{}]", sourcePath, destPath);
    }

    try {
      Path destParent = destPath.getParent();
      if (destParent != null && Files.exists(sourcePath)) {
        Files.createDirectories(destParent);
      }
      Files.copy(
          sourcePath,
          destPath,
          StandardCopyOption.COPY_ATTRIBUTES,
          StandardCopyOption.REPLACE_EXISTING);
      return true;
    } catch (IOException e) {
      log.error("Error copying/updating: [%s] -> [%s]".formatted(sourcePath, destPath), e);
      return false;
    }
  }

  @Override
  public boolean delete(String key) {
    Path destPath = destination.root().resolve(key);
    log.info("Deleting: [{}]", destPath);
    return deleteRecursively(destPath);
  }

  private boolean deleteRecursively(Path destPath) {
    try {
      boolean allDeleted = true;
      if (Files.isDirectory(destPath)) {
        try (Stream<Path> files = Files.list(destPath)) {
          allDeleted = files.allMatch(this::deleteRecursively);
        }
      }
      Files.deleteIfExists(destPath);
      return allDeleted;
    } catch (IOException e) {
      log.error("Error deleting: [%s]".formatted(destPath), e);
      return false;
    }
  }
}
