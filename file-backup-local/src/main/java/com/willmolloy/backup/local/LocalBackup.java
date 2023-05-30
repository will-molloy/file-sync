package com.willmolloy.backup.local;

import static java.util.Objects.requireNonNull;

import com.willmolloy.backup.BaseBackup;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * For backups to/from locally mounted storage. Either local disk, or mounted NAS, etc.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
final class LocalBackup extends BaseBackup<LocalFile, LocalFile> {

  private static final Logger log = LogManager.getLogger();

  private final LocalStorage destination;

  LocalBackup(LocalStorage source, LocalStorage destination) {
    super(source, destination);
    this.destination = requireNonNull(destination);
  }

  @Override
  public boolean put(LocalFile sourceFile) {
    Path sourcePath = sourceFile.fullPath();
    Path destPath = destination.root().resolve(sourceFile.relativePath());
    try {
      // TODO use ancestors to copy parent dirs to ensure last-modified (& other attributes) synced?
      return createParentDirs(sourcePath, destPath) && doCopy(sourcePath, destPath);
    } catch (Exception e) {
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
      // TODO if we do the deletes first, we won't end up in these scenarios? Good to be safe?
      // failed to create directory since it already exists as a file
      log.warn(
          "Error copying: [{}] -> [{}]. Deleting file to allow creation of directories first",
          sourcePath,
          destPath,
          e);
      FileSystem fs = destination.root().getFileSystem();
      Path badPath = destination.root().relativize(fs.getPath(e.getFile()));
      return delete(getDestFile(badPath)) && createParentDirs(sourcePath, destPath);
    } catch (NoSuchFileException e) {
      // same as above, except its thrown when the parent already exists as a file
      // (see https://stackoverflow.com/a/76278968/6122976)
      log.warn(
          "Error copying: [{}] -> [{}]. Deleting file to allow creation of directories first",
          sourcePath,
          destPath,
          e);
      FileSystem fs = destination.root().getFileSystem();
      // for some reason e.getFile here is in absolute form, so take that into account
      // TODO what if the jdk changes this behaviour? create a 'safe relativize' method?
      Path badPath =
          destination.root().toAbsolutePath().relativize(fs.getPath(e.getFile()).getParent());
      return delete(getDestFile(badPath)) && createParentDirs(sourcePath, destPath);
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
      FileSystem fs = destination.root().getFileSystem();
      Path badPath = destination.root().relativize(fs.getPath(e.getFile()));
      return delete(getDestFile(badPath)) && doCopy(sourcePath, destPath);
    }
  }

  private LocalFile getDestFile(Path relativePath) {
    return destination
        .fileTree()
        .get(relativePath)
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "Path [%s] not in destination file tree".formatted(relativePath)));
  }

  @Override
  public boolean delete(LocalFile destFile) {
    AtomicBoolean allDeleted = new AtomicBoolean(true);
    try {
      destination
          .fileTree()
          .subtree(destFile)
          .postorder()
          .map(LocalFile::fullPath)
          .forEach(
              destPath -> {
                try {
                  Files.delete(destPath);
                  log.info("Deleted: [{}]", destPath);
                } catch (NoSuchFileException e) {
                  log.debug("Already deleted: [{}]", destPath, e);
                } catch (Exception e) {
                  log.error("Error deleting: [{}]", destPath, e);
                  allDeleted.set(false);
                }
              });
    } catch (Exception e) {
      log.error("Error deleting: [{}]", destFile.uri(), e);
      return false;
    }
    return allDeleted.get();
  }

  @Override
  public boolean syncAttributes(LocalFile sourceFile, LocalFile destFile) {
    try {
      Path destPath = destFile.fullPath();
      Files.setLastModifiedTime(destPath, FileTime.fromMillis(sourceFile.lastModified()));
      return true;
    } catch (Exception e) {
      log.error("Error setting last-modified time: [{}]", destFile, e);
      return false;
    }
  }
}
