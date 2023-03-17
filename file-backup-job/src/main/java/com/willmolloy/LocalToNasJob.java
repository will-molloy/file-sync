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
public record LocalToNasJob(Path sourceRoot, Path destinationRoot) implements Job {

  private static final Logger log = LogManager.getLogger();

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
