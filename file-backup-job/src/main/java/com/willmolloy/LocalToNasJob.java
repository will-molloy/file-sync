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
  private final Path destinationRoot;

  public LocalToNasJob(Path sourceRoot, Path destinationRoot) {
    log.debug("LocalToNasJob({}, {})", sourceRoot, destinationRoot);
    this.sourceRoot = sourceRoot;
    this.destinationRoot = destinationRoot;
  }

  @Override
  public Stream<Path> scanSource() {
    log.debug("scanSource({})", sourceRoot);
    try {
      return Files.walk(sourceRoot)
          // skip self
          .skip(1)
          // strip prefix so can compare with destination
          .map(sourceRoot::relativize);
    } catch (IOException e) {
      log.error("Error scanning source", e);
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public Stream<Path> scanDestination() {
    log.debug("scanDestination({})", destinationRoot);
    try {
      return Files.walk(destinationRoot).skip(1).map(destinationRoot::relativize);
    } catch (IOException e) {
      log.error("Error scanning destination", e);
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void copyToDestination(Path file) {
    log.debug("copyToDestination({})", file);
    Path sourceFile = sourceRoot.resolve(file);
    Path destinationFile = destinationRoot.resolve(file);
    try {
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
    Path destinationFile = destinationRoot.resolve(file);
    try {
      Files.delete(destinationFile);
    } catch (IOException e) {
      log.error("Error deleting from destination", e);
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public boolean sourceNotEqualDestination(Path file) {
    log.debug("sourceNotEqualDestination({})", file);
    Path sourcePath = sourceRoot.resolve(file);
    Path destinationPath = destinationRoot.resolve(file);
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
