package com.willmolloy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
      // skip self
      return Files.walk(sourceRoot).skip(1).toList();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public List<Path> scanDestination() {
    log.debug("scanDestination({})", destinationRoot);
    try {
      return Files.walk(destinationRoot).skip(1).toList();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void copyToDestination(Path sourceFile, Path destinationLocation) {
    log.debug("copyToDestination({}, {})", sourceFile, destinationLocation);
    try {
      Files.copy(sourceFile, destinationLocation);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void deleteFromDestination(Path destinationFile) {
    log.debug("deleteFromDestination({})", destinationFile);
    try {
      Files.delete(destinationFile);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
