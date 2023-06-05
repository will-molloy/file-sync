package com.willmolloy.backup;

import static com.willmolloy.backup.util.Preconditions.require;
import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Base {@link File} class with common methods implemented for convenience.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public abstract class BaseFile implements File {

  private final Path relativePath;
  private final boolean isDirectory;
  private final long size;

  protected BaseFile(Path relativePath, boolean isDirectory, long size) {
    this.relativePath = requireNonNull(relativePath);
    this.isDirectory = isDirectory;
    require(size >= 0, "Requires non-negative size");
    this.size = size;
  }

  @Override
  public final Path relativePath() {
    return relativePath;
  }

  @Override
  public final boolean isDirectory() {
    return isDirectory;
  }

  @Override
  public final long size() {
    return size;
  }

  @Override
  public final boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    return o instanceof File file && Objects.equals(uri(), file.uri());
  }

  @Override
  public final int hashCode() {
    return Objects.hash(uri());
  }

  @Override
  public final String toString() {
    return "%s[%s]".formatted(getClass().getSimpleName(), uri());
  }
}
