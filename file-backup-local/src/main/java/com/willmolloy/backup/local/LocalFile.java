package com.willmolloy.backup.local;

import static com.willmolloy.backup.util.Preconditions.require;
import static java.util.Objects.requireNonNull;

import com.willmolloy.backup.BaseFile;
import com.willmolloy.backup.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Local file on disk.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public final class LocalFile extends BaseFile {

  static LocalFile fromAttributes(
      LocalStorage localStorage, Path path, BasicFileAttributes attributes) {
    require(
        path.startsWith(localStorage.root()),
        "Requires path [%s] to be under root [%s]".formatted(path, localStorage.root()));
    require(
        attributes.isRegularFile() || attributes.isDirectory(),
        "Requires a file or directory: [%s]".formatted(path));
    return new LocalFile(
        localStorage,
        localStorage.root().relativize(path),
        attributes.isDirectory(),
        attributes.size(),
        attributes.lastModifiedTime().toMillis());
  }

  public static LocalFile fromPath(LocalStorage localStorage, Path path) throws IOException {
    return fromAttributes(
        localStorage, path, Files.readAttributes(path, BasicFileAttributes.class));
  }

  static LocalFile directoryFiller(LocalStorage localStorage, String relativePath) {
    FileSystem fs = localStorage.root().getFileSystem();
    return new LocalFile(localStorage, fs.getPath(relativePath), true, 0, 0);
  }

  private final LocalStorage localStorage;
  private final Path relativePath;
  private final boolean isDirectory;
  private final long size;
  private final long lastModified;

  private LocalFile(
      LocalStorage localStorage,
      Path relativePath,
      boolean isDirectory,
      long size,
      long lastModified) {
    this.localStorage = requireNonNull(localStorage);
    this.relativePath = requireNonNull(relativePath);
    this.isDirectory = isDirectory;
    require(size >= 0, "Requires non-negative size");
    this.size = size;
    this.lastModified = lastModified;
  }

  @Override
  public String uri() {
    return fullPath().toString();
  }

  @Override
  public Path relativePath() {
    return relativePath;
  }

  @Override
  public boolean isDirectory() {
    return isDirectory;
  }

  @Override
  public long size() {
    return size;
  }

  public Path fullPath() {
    return localStorage.root().resolve(relativePath);
  }

  long lastModified() {
    return lastModified;
  }

  @Override
  public boolean same(File other) {
    if (other instanceof LocalFile localFile) {
      return super.same(other) && lastModified == localFile.lastModified;
    }
    return super.same(other);
  }
}
