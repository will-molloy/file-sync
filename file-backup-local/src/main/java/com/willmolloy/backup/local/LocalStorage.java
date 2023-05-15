package com.willmolloy.backup.local;

import static com.willmolloy.backup.util.Preconditions.require;
import static java.util.Objects.requireNonNull;

import com.sun.source.tree.Tree;
import com.willmolloy.backup.Backup;
import com.willmolloy.backup.Backup.Location;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
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
  public NavigableMap<Path, Backup.Node> scan() {
    log.info("Scanning directory: [{}]", root);
    try {
      TreeMap<Path, Backup.Node> map = new TreeMap<>();

      Function<Path, Path> keyFunc = root::relativize;

      // not sure how duplicates occur?? But it does happen; take the most recently scanned file.
      BinaryOperator<Backup.Node> mergeFunc =
          (first, second) -> {
            log.warn("Scanned duplicate: [{}]", second);
            return second;
          };

      BiConsumer<Path, BasicFileAttributes> fileConsumer =
          (path, attributes) -> {
            Path key = keyFunc.apply(path);
            LocalFile localFile = new LocalFile(path, attributes);
            map.merge(key, localFile, mergeFunc);
          };

      BiConsumer<Path, BasicFileAttributes> directoryConsumer =
          (path, attributes) -> {
            if (path == root) {
              return;
            }
            Path key = keyFunc.apply(path);
            LocalDirectory localFile = new LocalDirectory(path, attributes);
            map.merge(key, localFile, mergeFunc);
          };

      Files.walkFileTree(root, new DirectoryScanner(fileConsumer, directoryConsumer));
      return map;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public String toString() {
    return "%s[%s]".formatted(getClass().getSimpleName(), root);
  }

  private static final class DirectoryScanner implements FileVisitor<Path> {
    private final BiConsumer<Path, BasicFileAttributes> fileConsumer;
    private final BiConsumer<Path, BasicFileAttributes> directoryConsumer;

    private DirectoryScanner(
        BiConsumer<Path, BasicFileAttributes> fileConsumer,
        BiConsumer<Path, BasicFileAttributes> directoryConsumer) {
      this.fileConsumer = requireNonNull(fileConsumer);
      this.directoryConsumer = requireNonNull(directoryConsumer);
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attributes) {
      directoryConsumer.accept(dir, attributes);
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
      // TODO handle symlinks?
      if (attributes.isSymbolicLink()) {
        log.warn("Skipped file (symlink): [{}]", file);
        return FileVisitResult.CONTINUE;
      }
      fileConsumer.accept(file, attributes);
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
