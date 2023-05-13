package com.willmolloy.backup.local;

import static com.willmolloy.backup.util.Preconditions.require;
import static java.util.Objects.requireNonNull;

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
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
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
public record LocalStorage(Path root) implements Location<LocalFile> {

  private static final Logger log = LogManager.getLogger();

  public LocalStorage {
    requireNonNull(root);
    require(Files.isDirectory(root), "Requires a directory: [%s]".formatted(root));
  }

  @Override
  public Map<String, LocalFile> scan() {
    log.info("Scanning directory: [{}]", root);
    try {
      Map<String, LocalFile> map = new HashMap<>();

      Function<Path, String> keyFunc =
          ((Function<Path, Path>) root::relativize).andThen(LocalStorage::ensureUnixSeparator);

      // not sure how duplicates occur?? But it does happen; take the most recently scanned file.
      BiFunction<LocalFile, LocalFile, LocalFile> mergeFunc =
          (first, second) -> {
            log.warn("Scanned duplicate: [{}]", second.path());
            return second;
          };

      BiConsumer<Path, BasicFileAttributes> consumer =
          (path, attributes) -> {
            if (path == root) {
              return;
            }

            String key = keyFunc.apply(path);
            LocalFile localFile = new LocalFile(path, attributes);
            map.merge(key, localFile, mergeFunc);
          };

      Files.walkFileTree(root, new DirectoryScanner(consumer));
      return map;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static String ensureUnixSeparator(Path path) {
    if (File.separatorChar == '/') {
      return path.toString();
    } else {
      return StreamSupport.stream(path.spliterator(), false)
          .map(Path::toString)
          .collect(Collectors.joining("/"));
    }
  }

  @Override
  public String toString() {
    return "%s[%s]".formatted(getClass().getSimpleName(), root);
  }

  private static final class DirectoryScanner implements FileVisitor<Path> {
    private final BiConsumer<Path, BasicFileAttributes> consumer;

    private DirectoryScanner(BiConsumer<Path, BasicFileAttributes> consumer) {
      this.consumer = requireNonNull(consumer);
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attributes) {
      if (!attributes.isDirectory()) {
        log.warn(
            "Skipped directory (not actually a directory): [{}]. Attributes: [{}]",
            dir,
            attributes);
        return FileVisitResult.CONTINUE;
      }
      consumer.accept(dir, attributes);
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
      // TODO handle symlinks?
      if (!attributes.isRegularFile()) {
        log.warn("Skipped file (not actually a file): [{}]. Attributes: [{}]", file, attributes);
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
