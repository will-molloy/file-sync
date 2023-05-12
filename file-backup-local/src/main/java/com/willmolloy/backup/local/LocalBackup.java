package com.willmolloy.backup.local;

import static java.util.Objects.requireNonNull;

import com.willmolloy.backup.Backup;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * For backups to/from locally mounted storage. Either local disk, or mounted NAS, etc.
 *
 * @param source source
 * @param destination destination
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
record LocalBackup(LocalStorage source, LocalStorage destination)
    implements Backup<LocalStorage, LocalStorage> {

  private static final Logger log = LogManager.getLogger();

  LocalBackup(LocalStorage source, LocalStorage destination) {
    this.source = requireNonNull(source);
    this.destination = requireNonNull(destination);
  }

  @Override
  public boolean put(String key) {
    Path sourcePath = source.root().resolve(key);
    Path destPath = destination.root().resolve(key);
    try {
      Path destParent = destPath.getParent();
      if (destParent != null) {
        Files.createDirectories(destParent);
      }
      Files.copy(
          sourcePath,
          destPath,
          StandardCopyOption.COPY_ATTRIBUTES,
          StandardCopyOption.REPLACE_EXISTING);
      log.info("Copied: [{}] -> [{}]", sourcePath, destPath);
      return true;
    } catch (DirectoryNotEmptyException e) {
      log.warn(
          "Error copying: [{}] -> [{}]. Deleting non-empty directory on destination first",
          sourcePath,
          destPath);
      return delete(key) && put(key);
    } catch (IOException e) {
      log.error("Error copying: [%s] -> [%s]".formatted(sourcePath, destPath), e);
      return false;
    }
  }

  @Override
  public boolean delete(String key) {
    Path destPath = destination.root().resolve(key);
    try {
      Files.walkFileTree(destPath, new DirectoryCleaner());
      log.info("Deleted: [{}]", destPath);
      return true;
    } catch (NoSuchFileException ignored) {
      return true;
    } catch (IOException e) {
      log.error("Error deleting: [%s]".formatted(destPath), e);
      return false;
    }
  }

  // multiple threads are running recursive delete (recursive is the only way to make this
  // thread-safe - can't delete in order)
  // so ignore 'NoSuchFileException' as another thread may have got there first.
  private static final class DirectoryCleaner implements FileVisitor<Path> {
    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attributes) {
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
      Files.deleteIfExists(file);
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException e) throws IOException {
      if (e instanceof NoSuchFileException) {
        return FileVisitResult.CONTINUE;
      } else {
        log.error("Error visiting file: [%s]".formatted(file), e);
        throw e;
      }
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
      if (e != null) {
        if (e instanceof NoSuchFileException) {
          return FileVisitResult.CONTINUE;
        }
        log.error("Error visiting directory: [%s]".formatted(dir), e);
        throw e;
      }
      Files.deleteIfExists(dir);
      return FileVisitResult.CONTINUE;
    }
  }
}
