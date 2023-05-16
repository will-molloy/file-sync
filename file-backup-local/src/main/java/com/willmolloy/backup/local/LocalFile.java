package com.willmolloy.backup.local;

import static com.willmolloy.backup.util.Preconditions.require;
import static java.util.Objects.requireNonNull;

import com.willmolloy.backup.FileTree;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.Objects;

/**
 * Local file on disk.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
class LocalFile implements FileTree.File {

  private final Path path;
  private final long size;
  private final Instant lastModified;
  private final boolean isDirectory;

  LocalFile(Path path, BasicFileAttributes attributes) {
    require(
        attributes.isRegularFile() || attributes.isDirectory(),
        "Requires a file or directory: [%s]".formatted(path));
    this.path = requireNonNull(path);
    this.size = attributes.size();
    this.lastModified = attributes.lastModifiedTime().toInstant();
    this.isDirectory = attributes.isDirectory();
  }

  LocalFile(Path path) throws IOException {
    this(path, Files.readAttributes(path, BasicFileAttributes.class));
  }

  @Override
  public long size() {
    return size;
  }

  Instant lastModified() {
    return lastModified;
  }

  @Override
  public boolean isDirectory() {
    return isDirectory;
  }

  @Override
  public boolean same(FileTree.File other) {
    if (other instanceof LocalFile localFile) {
      return FileTree.File.super.same(other) && lastModified.equals(localFile.lastModified);
    }
    return FileTree.File.super.same(other);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LocalFile localFile = (LocalFile) o;
    return Objects.equals(path, localFile.path);
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
