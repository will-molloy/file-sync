package com.willmolloy.sync.local;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.willmolloy.sync.BaseFile;
import java.io.IOException;
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
    checkArgument(
        path.startsWith(localStorage.root()),
        "Requires path [%s] to be under root [%s]",
        path,
        localStorage.root());
    checkArgument(
        attributes.isRegularFile() || attributes.isDirectory(),
        "Requires a file or directory: [%s]",
        path);
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

  private final LocalStorage localStorage;
  private final Path relativePath;
  private final long lastModified;

  private LocalFile(
      LocalStorage localStorage,
      Path relativePath,
      boolean isDirectory,
      long size,
      long lastModified) {
    super(relativePath, isDirectory, size);
    this.localStorage = checkNotNull(localStorage);
    this.relativePath = checkNotNull(relativePath);
    this.lastModified = lastModified;
  }

  @Override
  public String uri() {
    return fullPath().toString();
  }

  public Path fullPath() {
    return localStorage.root().resolve(relativePath);
  }

  long lastModified() {
    return lastModified;
  }
}
