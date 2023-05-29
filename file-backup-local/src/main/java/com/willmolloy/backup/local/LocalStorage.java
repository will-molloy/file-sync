package com.willmolloy.backup.local;

import static com.willmolloy.backup.util.Preconditions.require;
import static java.util.Objects.requireNonNull;

import com.willmolloy.backup.FileTree;
import com.willmolloy.backup.Location;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.BiConsumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Represents a local storage location.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public final class LocalStorage implements Location<LocalFile> {

  private static final Logger log = LogManager.getLogger();

  private final Path rootDir;

  public LocalStorage(Path rootDir) {
    this.rootDir = requireNonNull(rootDir);
    require(Files.isDirectory(rootDir), "Requires a directory: [%s]".formatted(rootDir));
  }

  @Override
  public FileTree<LocalFile> scan() {
    log.info("Scanning directory: [{}]", rootDir);
    try {
      FileTree.Builder<LocalFile> builder = FileTree.builder(LocalFile.fromPath(this, rootDir));
      BiConsumer<Path, BasicFileAttributes> consumer =
          (path, attributes) -> {
            if (path == rootDir) {
              return;
            }
            LocalFile file = LocalFile.fromAttributes(this, path, attributes);
            builder.insert(file);
          };
      Files.walkFileTree(rootDir, new DirectoryWalker(consumer));
      return builder.build();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public Path root() {
    return rootDir;
  }

  @Override
  public String toString() {
    return "%s[%s]".formatted(getClass().getSimpleName(), rootDir);
  }

  private static final class DirectoryWalker implements FileVisitor<Path> {
    private final BiConsumer<Path, BasicFileAttributes> consumer;

    private DirectoryWalker(BiConsumer<Path, BasicFileAttributes> consumer) {
      this.consumer = requireNonNull(consumer);
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attributes) {
      consumer.accept(dir, attributes);
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
      // TODO handle symlinks?
      if (attributes.isSymbolicLink()) {
        log.warn("Skipped file (symlink): [{}]", file);
        return FileVisitResult.CONTINUE;
      }
      consumer.accept(file, attributes);
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException e) {
      if (e instanceof AccessDeniedException) {
        log.warn("Skipped file (access denied): [{}]", file);
      } else {
        log.error("Error visiting file: [{}]", file, e);
      }
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException e) {
      if (e != null) {
        log.error("Error visiting directory: [{}]", dir, e);
      }
      return FileVisitResult.CONTINUE;
    }
  }
}
