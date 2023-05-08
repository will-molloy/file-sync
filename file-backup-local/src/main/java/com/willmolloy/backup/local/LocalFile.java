package com.willmolloy.backup.local;

import static com.willmolloy.backup.util.Md5Helper.md5Base16;
import static com.willmolloy.backup.util.Preconditions.require;
import static java.util.Objects.requireNonNull;

import com.willmolloy.backup.Backup.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Local file on disk.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
class LocalFile implements File {

  private static final Logger log = LogManager.getLogger();

  private final Path path;
  private final long size;
  private final Instant lastModified;

  LocalFile(Path path) {
    try {
      this.path = requireNonNull(path);
      // more efficient to read attributes once
      BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
      require(attributes.isRegularFile(), "Requires a file: [%s]".formatted(path));
      this.size = attributes.size();
      this.lastModified = attributes.lastModifiedTime().toInstant();
    } catch (IOException e) {
      log.error("Error reading file attributes", e);
      throw new UncheckedIOException(e);
    }
  }

  public Path path() {
    return path;
  }

  @Override
  public long size() {
    return size;
  }

  @Override
  public Instant lastModified() {
    return lastModified;
  }

  @Override
  public String etag() {
    try {
      return "\"%s\"".formatted(md5Base16(path));
    } catch (IOException e) {
      log.error("Error computing MD5 Digest of file: [%s]".formatted(path), e);
      return "";
    }
  }

  @Override
  public boolean sameContents(File other) {
    if (other instanceof LocalFile localFile) {
      return size() == other.size() && lastModified().equals(localFile.lastModified());
    }
    return File.super.sameContents(other);
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
