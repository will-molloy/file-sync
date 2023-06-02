package com.willmolloy.backup.local;

import static com.willmolloy.backup.util.Preconditions.require;
import static java.util.Objects.requireNonNull;

import com.willmolloy.backup.BaseFile;
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
    this.localStorage = requireNonNull(localStorage);
    this.relativePath = requireNonNull(relativePath);
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
