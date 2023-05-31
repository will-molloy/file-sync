package com.willmolloy.backup.local;

import static java.util.Objects.requireNonNull;

import com.willmolloy.backup.BaseBackup;
import com.willmolloy.backup.FileTree;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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

  LocalBackup(LocalStorage source, LocalStorage destination) {
    super(source, destination);
    this.destination = requireNonNull(destination);
  }

  @Override
  public boolean put(LocalFile sourceFile) {
    Path sourcePath = sourceFile.fullPath();
    Path destPath = destination.root().resolve(sourceFile.relativePath());
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
  public boolean needDelete(LocalFile destFile) {
    FileTree<LocalFile> destFileTree = destination().fileTree();
    // don't delete the root, it was created manually outside this app; if it's deleted subsequent
    // runs will fail
    if (destFileTree.isRoot(destFile)) {
      return false;
    }

    FileTree<LocalFile> sourceFileTree = source().fileTree();
    Optional<LocalFile> maybeSourceFile = sourceFileTree.get(destFile.relativePath());
    // either file not on source -> delete
    // OR files different -> need to delete before update, otherwise there are scenarios where it
    // can fail, e.g. non-empty dir overwriting a file
    return maybeSourceFile.isEmpty() || !maybeSourceFile.get().same(destFile);
  }
}
