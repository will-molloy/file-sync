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
    log.debug("scanSource({})", sourceRoot);
    try {
      return Files.walk(sourceRoot)
          // skip self
          .skip(1)
          // leaves only - copy method will create parent directories as required
          .filter(Helpers::isLeaf)
          // strip prefix so can compare with destination
          .map(sourceRoot::relativize);
    } catch (IOException e) {
      log.error("Error scanning source", e);
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public Stream<Path> scanDestination() {
    log.debug("scanDestination({})", destRoot);
    try {
      return Files.walk(destRoot).skip(1).map(destRoot::relativize);
    } catch (IOException e) {
      log.error("Error scanning destination", e);
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void copyToDestination(Path file) {
    log.debug("copyToDestination({})", file);
    Path sourceFile = sourceRoot.resolve(file);
    Path destinationFile = destRoot.resolve(file);
    try {
      Path destinationParent = destinationFile.getParent();
      if (destinationParent != null) {
        Files.createDirectories(destinationParent);
      }
      Files.copy(
          sourceFile,
          destinationFile,
          StandardCopyOption.REPLACE_EXISTING,
          StandardCopyOption.COPY_ATTRIBUTES);
    } catch (IOException e) {
      log.error("Error copying to destination", e);
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void deleteFromDestination(Path file) {
    log.debug("deleteFromDestination({})", file);
    Path destinationFile = destRoot.resolve(file);
    try {
      delete(destinationFile);
    } catch (IOException e) {
      log.error("Error deleting from destination", e);
      throw new UncheckedIOException(e);
    }
  }

  private void delete(Path path) throws IOException {
    if (Files.isDirectory(path)) {
      for (Path child : Files.list(path).toList()) {
        delete(child);
      }
    }
    Files.deleteIfExists(path);
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
