package com.willmolloy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Helper methods.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public final class Helpers {

  private static final Logger log = LogManager.getLogger();

  /** {@code true} if {@code path} is a leaf. */
  public static boolean isLeaf(Path path) {
    if (Files.isRegularFile(path)) {
      return true;
    }
    try (Stream<Path> list = Files.list(path)) {
      return list.findAny().isEmpty();
    } catch (IOException e) {
      log.error("Error listing directory", e);
      throw new UncheckedIOException(e);
    }
  }

  private Helpers() {}
}
