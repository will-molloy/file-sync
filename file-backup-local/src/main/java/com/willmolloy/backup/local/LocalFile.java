package com.willmolloy.backup.local;

import static com.willmolloy.backup.util.Preconditions.require;
import static java.util.Objects.requireNonNull;

import com.willmolloy.backup.BaseFile;
import com.willmolloy.backup.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;

/**
 * Local file on disk.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public final class LocalFile extends BaseFile implements File {

  private final LocalStorage localStorage;
  private final Path relativePath;
  private final long size;
  private final boolean isDirectory;
  private final Instant lastModified;

  LocalFile(LocalStorage localStorage, Path path, BasicFileAttributes attributes) {
    require(
        path.startsWith(localStorage.root()),
        "Requires path [%s] to be under root [%s]".formatted(path, localStorage.root()));
    this.localStorage = requireNonNull(localStorage);
    this.relativePath = localStorage.root().relativize(path);

    require(
        attributes.isRegularFile() || attributes.isDirectory(),
        "Requires a file or directory: [%s]".formatted(path));
    this.size = attributes.size();
    this.isDirectory = attributes.isDirectory();
    this.lastModified = attributes.lastModifiedTime().toInstant();
  }

  public LocalFile(LocalStorage localStorage, Path path) throws IOException {
    this(localStorage, path, Files.readAttributes(path, BasicFileAttributes.class));
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

  Instant lastModified() {
    return lastModified;
  }

  @Override
  public boolean same(File other) {
    if (other instanceof LocalFile localFile) {
      return super.same(other) && lastModified.equals(localFile.lastModified);
    }
    return super.same(other);
  }
}
