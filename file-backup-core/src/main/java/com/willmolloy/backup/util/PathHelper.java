package com.willmolloy.backup.util;

import static java.util.function.Predicate.not;

import com.google.common.collect.Streams;
import java.io.File;
import java.nio.file.Path;
import java.util.List;

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
    return Streams.stream(path).map(Path::toString).filter(not(String::isEmpty)).toList();
  }

  private PathHelper() {}
}
