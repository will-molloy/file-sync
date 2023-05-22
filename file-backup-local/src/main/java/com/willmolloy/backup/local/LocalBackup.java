package com.willmolloy.backup.local;

import static java.util.Objects.requireNonNull;

import com.willmolloy.backup.Backup;
import com.willmolloy.backup.File;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
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
  public boolean put(File sourceFile) {
    Path key = sourceFile.relativePath();
    Path sourcePath = source.root().resolve(key);
    Path destPath = destination.root().resolve(key);
    return robustCopy(sourcePath, destPath);
  }

  private boolean robustCopy(Path sourcePath, Path destPath) {
    try {
      return createParentDirs(sourcePath, destPath) && doCopy(sourcePath, destPath);
    } catch (IOException e) {
      log.error("Error copying: [{}] -> [{}]", sourcePath, destPath, e);
      return false;
    }
  }

  private boolean createParentDirs(Path sourcePath, Path destPath) throws IOException {
    try {
      Path destParent = destPath.getParent();
      if (destParent != null) {
        Files.createDirectories(destParent);
      }
      return true;
    } catch (FileAlreadyExistsException e) {
      // failed to create directory since it already exists as a file
      FileSystem fs = destination.root().getFileSystem();
      Path badPath = fs.getPath(e.getFile());
      log.warn(
          "Error copying: [{}] -> [{}]. Deleting file [{}] to allow creation of directories first",
          sourcePath,
          destPath,
          badPath,
          e);
      return robustDelete(badPath) && createParentDirs(sourcePath, destPath);
    } catch (NoSuchFileException e) {
      // same as above, except its thrown when the parent already exists as a file
      // (see https://stackoverflow.com/a/76278968/6122976)
      FileSystem fs = destination.root().getFileSystem();
      Path badPath = fs.getPath(e.getFile()).getParent();
      log.warn(
          "Error copying: [{}] -> [{}]. Deleting file [{}] to allow creation of directories first",
          sourcePath,
          destPath,
          badPath,
          e);
      return robustDelete(badPath) && createParentDirs(sourcePath, destPath);
    }
  }

  private boolean doCopy(Path sourcePath, Path destPath) throws IOException {
    try {
      Files.copy(
          sourcePath,
          destPath,
          StandardCopyOption.COPY_ATTRIBUTES,
          StandardCopyOption.REPLACE_EXISTING);
      log.info("Copied: [{}] -> [{}]", sourcePath, destPath);
      return true;
    } catch (NoSuchFileException e) {
      log.warn(
          "Skipped copy: [{}] -> [{}]. Source file deleted since scan", sourcePath, destPath, e);
      return true;
    } catch (DirectoryNotEmptyException e) {
      log.warn(
          "Error copying: [{}] -> [{}]. Deleting non-empty directory on destination first",
          sourcePath,
          destPath,
          e);
      return robustDelete(destPath) && doCopy(sourcePath, destPath);
    }
  }

  @Override
  public boolean delete(File destFile) {
    Path key = destFile.relativePath();
    Path destPath = destination.root().resolve(key);
    return robustDelete(destPath);
  }

  private boolean robustDelete(Path destPath) {
    try {
      Files.walkFileTree(destPath, new RecursiveDelete());
      log.info("Deleted: [{}]", destPath);
      return true;
    } catch (NoSuchFileException e) {
      log.debug("Already deleted: [{}]", destPath, e);
      return true;
    } catch (IOException e) {
      log.error("Error deleting: [{}]", destPath, e);
      return false;
    }
  }

  // multiple threads are running recursive delete
  // (this is the only way to run in parallel - can't guarantee the order)
  // so ignore 'NoSuchFileException' as another thread may have got there first.
  private static final class RecursiveDelete implements FileVisitor<Path> {
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
        log.debug("Already deleted file: [{}]", file, e);
        return FileVisitResult.CONTINUE;
      } else {
        log.error("Error visiting file: [{}]", file, e);
        throw e;
      }
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
      if (e != null) {
        if (e instanceof NoSuchFileException) {
          log.debug("Already deleted directory: [{}]", dir, e);
          return FileVisitResult.CONTINUE;
        }
        log.error("Error visiting directory: [{}]", dir, e);
        throw e;
      }
      Files.deleteIfExists(dir);
      return FileVisitResult.CONTINUE;
    }
  }
}
