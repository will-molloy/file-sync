package com.willmolloy.backup.local;

import static com.willmolloy.backup.util.Preconditions.require;
import static java.util.Objects.requireNonNull;

import com.willmolloy.backup.Backup.Location;
import com.willmolloy.backup.FileTree;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Represents a local storage location.
 *
 * @param root root directory
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public record LocalStorage(Path root) implements Location {

  private static final Logger log = LogManager.getLogger();

  public LocalStorage {
    requireNonNull(root);
    require(Files.isDirectory(root), "Requires a directory: [%s]".formatted(root));
  }

  @Override
  public FileTree scan() {
    log.info("Scanning directory: [{}]", root);
    try {
      TreeMap<Path, LocalFile> map = new TreeMap<>();

      Function<Path, Path> keyFunc = root::relativize;

      // not sure how duplicates occur?? But it does happen; take the most recently scanned file.
      BinaryOperator<LocalFile> mergeFunc =
          (first, second) -> {
            log.warn("Scanned duplicate: [{}]", second);
            return second;
          };

      BiConsumer<Path, BasicFileAttributes> consumer =
          (path, attributes) -> {
            if (path == root) {
              return;
            }
            Path key = keyFunc.apply(path);
            LocalFile file = new LocalFile(path, attributes);
            map.merge(key, file, mergeFunc);
          };

      Files.walkFileTree(root, new DirectoryWalker(consumer));
      return FileTree.create(map);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public String toString() {
    return "%s[%s]".formatted(getClass().getSimpleName(), root);
  }

  private static final class DirectoryWalker implements FileVisitor<Path> {
    private final BiConsumer<Path, BasicFileAttributes> consumer;

    private DirectoryWalker(BiConsumer<Path, BasicFileAttributes> fileConsumer) {
      this.consumer = requireNonNull(fileConsumer);
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
        log.error("Error visiting file: [%s]".formatted(file), e);
      }
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException e) {
      if (e != null) {
        log.error("Error visiting directory: [%s]".formatted(dir), e);
      }
      return FileVisitResult.CONTINUE;
    }
  }
}
