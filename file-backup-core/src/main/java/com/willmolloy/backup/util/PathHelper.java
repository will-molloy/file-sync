package com.willmolloy.backup.util;

import static java.util.function.Predicate.not;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
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
      return String.join("/", nameComponents(path));
    }
  }

  /** {@link Path#iterator} as {@code List<String>}. */
  public static List<String> nameComponents(Path path) {
    return StreamSupport.stream(path.spliterator(), false)
        .map(Path::toString)
        .filter(not(String::isEmpty))
        .toList();
  }

  private PathHelper() {}
}
