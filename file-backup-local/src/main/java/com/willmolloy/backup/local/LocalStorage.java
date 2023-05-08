package com.willmolloy.backup.local;

import static com.willmolloy.backup.util.Preconditions.require;
import static java.util.Objects.requireNonNull;

import com.willmolloy.backup.Backup.File;
import com.willmolloy.backup.Backup.Location;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
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
  public Map<String, File> scan() {
    log.info("Scanning directory: [{}]", root);

    Map<String, File> map = new HashMap<>();

    Function<Path, String> keyFunc =
        ((Function<Path, Path>) root::relativize).andThen(LocalStorage::ensureUnixSeparator);

    // not sure how duplicates occur?? But it does happen; take the most recently scanned file.
    BiFunction<File, File, File> mergeFunc = (first, second) -> second;

    try {
      Files.walkFileTree(
          root,
          new FileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attributes) {
              return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
              if (!attributes.isRegularFile()) {
                // ignore symbolic links
                log.warn("Not a file: [{}]", file);
                return FileVisitResult.CONTINUE;
              }

              String key = keyFunc.apply(file);
              LocalFile localFile = new LocalFile(file, attributes);
              map.merge(key, localFile, mergeFunc);
              return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException e) {
              log.warn("Failed to visit file: [%s]".formatted(file), e);
              return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path directory, IOException e) {
              if (e != null) {
                log.warn("Failed to visit directory: [%s]".formatted(directory), e);
              }
              return FileVisitResult.CONTINUE;
            }
          });

      return map;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static String ensureUnixSeparator(Path path) {
    if (java.io.File.separatorChar == '/') {
      return path.toString();
    } else {
      List<String> nameElements =
          StreamSupport.stream(path.spliterator(), false).map(Path::toString).toList();
      return String.join("/", nameElements);
    }
  }

  @Override
  public String toString() {
    return "%s[%s]".formatted(getClass().getSimpleName(), root);
  }
}
