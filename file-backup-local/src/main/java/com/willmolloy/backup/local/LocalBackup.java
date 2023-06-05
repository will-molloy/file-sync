package com.willmolloy.backup.local;

import static java.util.Objects.requireNonNull;

import com.willmolloy.backup.BaseBackup;
import com.willmolloy.backup.FileTree;
import com.willmolloy.backup.statistics.BackupObserver;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
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

  LocalBackup(LocalStorage source, LocalStorage destination, List<BackupObserver> observers) {
    super(source, destination, observers);
    this.destination = requireNonNull(destination);
  }

  @Override
  protected boolean put(LocalFile sourceFile) {
    Path sourcePath = sourceFile.fullPath();
    Path destPath = destination.root().resolve(sourceFile.relativePath());
    try {
      log.debug("Copying: [{}] -> [{}]", sourcePath, destPath);
      Path destParent = destPath.getParent();
      if (destParent != null) {
        Files.createDirectories(destParent);
      }
      Files.copy(
          sourcePath,
          destPath,
          StandardCopyOption.COPY_ATTRIBUTES,
          StandardCopyOption.REPLACE_EXISTING);
      log.debug("Copied: [{}] -> [{}]", sourcePath, destPath);
      return true;
    } catch (NoSuchFileException e) {
      log.warn(
          "Skipped copy: [{}] -> [{}]. Source file deleted since scan", sourcePath, destPath, e);
      return true;
    } catch (Exception e) {
      log.error("Error copying: [{}] -> [{}]", sourcePath, destPath, e);
      return false;
    }
  }

  @Override
  protected boolean delete(FileTree<LocalFile> destSubtree) {
    AtomicBoolean allDeleted = new AtomicBoolean(true);
    destSubtree
        .postorder()
        .map(LocalFile::fullPath)
        .forEach(
            destPath -> {
              try {
                log.debug("Deleting: [{}]", destPath);
                Files.delete(destPath);
                log.debug("Deleted: [{}]", destPath);
              } catch (NoSuchFileException e) {
                log.debug("Already deleted: [{}]", destPath, e);
              } catch (Exception e) {
                log.error("Error deleting: [{}]", destPath, e);
                allDeleted.set(false);
              }
            });
    return allDeleted.get();
  }

  @Override
  protected boolean needUpdate(LocalFile sourceFile, LocalFile destFile) {
    return super.needUpdate(sourceFile, destFile)
        || sourceFile.lastModified() != destFile.lastModified();
  }

  @Override
  protected boolean needDelete(Optional<LocalFile> optionalSourceFile, LocalFile destFile) {
    // file not on source -> delete
    // OR one is file, one is dir -> need to delete before update, otherwise we get errors
    // overwriting non-empty dir, or failing to create dirs because file is in the way.
    return optionalSourceFile.isEmpty()
        || optionalSourceFile.get().isDirectory() != destFile.isDirectory();
  }
}
