package com.willmolloy.backup.local;

import static com.willmolloy.backup.util.Preconditions.require;
import static java.util.Objects.requireNonNull;

import com.willmolloy.backup.FileTree;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;

/**
 * Local directory on disk.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
class LocalDirectory implements FileTree.Node.Directory {

  private final Path path;

  LocalDirectory(Path path, BasicFileAttributes attributes) {
    require(attributes.isDirectory(), "Requires a directory: [%s]".formatted(path));
    this.path = requireNonNull(path);
  }

  LocalDirectory(Path path) throws IOException {
    this(path, Files.readAttributes(path, BasicFileAttributes.class));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LocalDirectory localDirectory = (LocalDirectory) o;
    return Objects.equals(path, localDirectory.path);
  }

  @Override
  public int hashCode() {
    return Objects.hash(path);
  }

  @Override
  public String toString() {
    return "%s[%s]".formatted(getClass().getSimpleName(), path);
  }
}
