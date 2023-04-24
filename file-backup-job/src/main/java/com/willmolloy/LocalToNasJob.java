package com.willmolloy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * For backups from Local PC to NAS.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
// TODO rename? It could be same PC to same PC (e.g. HDD) not necessarily a NAS.
public class LocalToNasJob implements Job {

  private static final Logger log = LogManager.getLogger();

  private final Path sourceRoot;
  private final Path destRoot;

  public LocalToNasJob(Path sourceRoot, Path destRoot) {
    log.debug("LocalToNasJob({}, {})", sourceRoot, destRoot);
    this.sourceRoot = sourceRoot;
    this.destRoot = destRoot;
  }

  @Override
  public Stream<Path> scanSource() {
    return scan(sourceRoot);
  }

  @Override
  public Stream<Path> scanDestination() {
    return scan(destRoot);
  }

  private static Stream<Path> scan(Path root) {
    log.debug("scan({})", root);
    try {
      return Files.walk(root)
          // skip self
          .skip(1)
          // strip prefix so can compare source & destination
          .map(root::relativize);
    } catch (IOException e) {
      log.error("Error scanning", e);
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void copy(Path file) {
    log.debug("copy({})", file);
    Path sourceFile = sourceRoot.resolve(file);
    Path destinationFile = destRoot.resolve(file);
    try {
      Path destinationParent = destinationFile.getParent();
      if (destinationParent != null) {
        Files.createDirectories(destinationParent);
      }
      Files.copy(sourceFile, destinationFile, StandardCopyOption.COPY_ATTRIBUTES);
    } catch (IOException e) {
      log.error("Error copying file [%s] to destination".formatted(file), e);
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void delete(Path file) {
    log.debug("delete({})", file);
    Path destinationFile = destRoot.resolve(file);
    try {
      deleteRecursively(destinationFile);
    } catch (IOException e) {
      log.error("Error deleting file [%s] from destination".formatted(file), e);
      throw new UncheckedIOException(e);
    }
  }

  private void deleteRecursively(Path path) throws IOException {
    if (Files.isDirectory(path)) {
      for (Path child : Files.list(path).toList()) {
        deleteRecursively(child);
      }
    }
    Files.deleteIfExists(path);
  }

  @Override
  public void update(Path file) {
    log.debug("update({})", file);
    Path sourceFile = sourceRoot.resolve(file);
    Path destinationFile = destRoot.resolve(file);
    try {
      Files.copy(
          sourceFile,
          destinationFile,
          StandardCopyOption.REPLACE_EXISTING,
          StandardCopyOption.COPY_ATTRIBUTES);
    } catch (IOException e) {
      log.error("Error updating file [%s] on destination".formatted(file), e);
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public boolean sourceNotEqualDestination(Path file) {
    log.debug("sourceNotEqualDestination({})", file);
    Path sourcePath = sourceRoot.resolve(file);
    Path destinationPath = destRoot.resolve(file);
    try {
      // this is sufficient? Files.mismatch is quite expensive.
      return Files.size(sourcePath) != Files.size(destinationPath)
          || Files.getLastModifiedTime(sourcePath)
                  .compareTo(Files.getLastModifiedTime(destinationPath))
              != 0;
    } catch (IOException e) {
      log.error("Error comparing source/dest", e);
      throw new UncheckedIOException(e);
    }
  }
}
