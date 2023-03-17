package com.willmolloy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
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
    this.sourceRoot = sourceRoot;
    this.destinationRoot = destinationRoot;
  }

  @Override
  public Path sourceRoot() {
    return sourceRoot;
  }

  @Override
  public Path destinationRoot() {
    return destinationRoot;
  }

  @Override
  public List<Path> scanSource() {
    log.debug("scanSource({})", sourceRoot);
    try {
      return Files.walk(sourceRoot)
          // skip self
          .skip(1)
          // strip prefix so can compare with destination
          .map(sourceRoot::relativize)
          // TODO return stream? Will need some kind of producer-consumer setup at some point...
          .toList();
    } catch (IOException e) {
      log.error("Error scanning source", e);
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public List<Path> scanDestination() {
    log.debug("scanDestination({})", destinationRoot);
    try {
      return Files.walk(destinationRoot).skip(1).map(destinationRoot::relativize).toList();
    } catch (IOException e) {
      log.error("Error scanning destination", e);
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void copyToDestination(Path sourceFile, Path destinationLocation) {
    log.debug("copyToDestination({}, {})", sourceFile, destinationLocation);
    try {
      Files.copy(
          sourceFile,
          destinationLocation,
          StandardCopyOption.REPLACE_EXISTING,
          StandardCopyOption.COPY_ATTRIBUTES);
    } catch (IOException e) {
      log.error("Error copying to destination", e);
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void deleteFromDestination(Path destinationFile) {
    log.debug("deleteFromDestination({})", destinationFile);
    try {
      Files.delete(destinationFile);
    } catch (IOException e) {
      log.error("Error deleting from destination", e);
      throw new UncheckedIOException(e);
    }
  }
}
