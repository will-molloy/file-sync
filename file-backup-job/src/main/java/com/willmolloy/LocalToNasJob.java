package com.willmolloy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
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
    log.debug("LocalToNasJob({}, {})", sourceRoot, destinationRoot);
    this.sourceRoot = sourceRoot;
    this.destinationRoot = destinationRoot;
  }

  @Override
  public List<String> scanSource() {
    log.debug("scanSource({})", sourceRoot);
    try {
      return Files.walk(sourceRoot)
          // skip self
          .skip(1)
          // strip prefix so can compare with destination
          .map(sourceRoot::relativize)
          .map(Path::toString)
          // TODO return stream? Will need some kind of producer-consumer setup at some point...
          .toList();
    } catch (IOException e) {
      log.error("Error scanning source", e);
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public List<String> scanDestination() {
    log.debug("scanDestination({})", destinationRoot);
    try {
      return Files.walk(destinationRoot)
          .skip(1)
          .map(destinationRoot::relativize)
          .map(Path::toString)
          .toList();
    } catch (IOException e) {
      log.error("Error scanning destination", e);
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void copyToDestination(String file) {
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
  public void deleteFromDestination(String file) {
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
  public boolean isNewerOnSource(String file) {
    log.debug("isNewerOnSource({})", file);
    Path sourcePath = sourceRoot.resolve(file);
    Path destinationPath = destinationRoot.resolve(file);
    try {
      FileTime sourceLastModified = Files.getLastModifiedTime(sourcePath);
      FileTime destLastModified = Files.getLastModifiedTime(destinationPath);
      return sourceLastModified.compareTo(destLastModified) > 0;
    } catch (IOException e) {
      log.error("Error getting last modified attribute", e);
      throw new UncheckedIOException(e);
    }
  }
}
