package com.willmolloy.sync.local;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Throwables;
import com.willmolloy.sync.FileTree;
import com.willmolloy.sync.Location;
import com.willmolloy.sync.util.concurrent.StructuredTaskScopeWrapper;
import com.willmolloy.sync.util.docker.DockerHelper;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
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
  private final String displayRootDir;

  public LocalStorage(Path rootDir) {
    this.rootDir = checkNotNull(rootDir);
    checkArgument(Files.isDirectory(rootDir), "Requires a directory: [%s]", rootDir);
    DockerHelper docker = new DockerHelper();
    displayRootDir = docker.getHostPath(rootDir.toString()).orElse(rootDir.toString());
  }

  @Override
  public FileTree<LocalFile> scan() {
    try {
      FileTree.Builder<LocalFile> builder =
          FileTree.builder(
              LocalFile.fromPath(this, rootDir), path -> LocalFile.directoryFiller(this, path));
      BiConsumer<Path, BasicFileAttributes> consumer =
          (path, attributes) -> {
            if (path == rootDir) {
              return;
            }
            LocalFile file = LocalFile.fromAttributes(this, path, attributes);
            builder.insert(file);
          };

      // limit thread count; if I/O is slow (e.g. network drive), it creates too many threads at
      // once and grinds to a halt
      // alternatively could use RecursiveAction (which uses a fixed sized ForkJoinPool) but found
      // StructuredTaskScope (virtual threads) is faster
      try (StructuredTaskScopeWrapper scope =
          new StructuredTaskScopeWrapper("scan", Runtime.getRuntime().availableProcessors())) {
        scope.fork(() -> walk(rootDir, consumer, scope));
      }

      return builder.build();
    } catch (Exception e) {
      log.error("Error scanning: [{}]", this, e);
      Throwables.throwIfUnchecked(e);
      throw new RuntimeException(e);
    }
  }

  public Path root() {
    return rootDir;
  }

  @Override
  public String toString() {
    return "%s[%s]".formatted(getClass().getSimpleName(), displayRootDir);
  }

  private void walk(
      Path rootDir,
      BiConsumer<Path, BasicFileAttributes> callback,
      StructuredTaskScopeWrapper scope)
      throws IOException {
    Files.walkFileTree(
        rootDir,
        new FileVisitor<>() {
          @Override
          public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attributes)
              throws IOException {
            if (rootDir == dir) {
              // only insert leaves (empty dirs and files); dirs covered by dir filler.
              try (Stream<Path> dirContents = Files.list(dir)) {
                if (dirContents.findAny().isEmpty()) {
                  callback.accept(dir, attributes);
                }
              }
              return FileVisitResult.CONTINUE;
            } else {
              // fork for each new directory discovered
              scope.fork(() -> walk(dir, callback, scope));
              return FileVisitResult.SKIP_SUBTREE;
            }
          }

          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
            // TODO handle symlinks?
            if (attributes.isSymbolicLink()) {
              log.warn("Skipped file (symlink): [{}]", file);
              return FileVisitResult.CONTINUE;
            }
            callback.accept(file, attributes);
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
        });
  }
}
