package com.willmolloy.backup.util;

import java.io.File;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Helper methods for {@link Path}.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public final class PathHelper {

  /** {@link Path#toString} ensuring unix {@code '/'} separator. */
  public static String ensureUnixSeparator(Path path) {
    if (File.separatorChar == '/') {
      return path.toString();
    } else {
      return StreamSupport.stream(path.spliterator(), false)
          .map(Path::toString)
          .collect(Collectors.joining("/"));
    }
  }

  private PathHelper() {}
}
