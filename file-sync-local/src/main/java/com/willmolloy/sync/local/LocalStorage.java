package com.willmolloy.sync.local;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Throwables;
import com.willmolloy.sync.FileTree;
import com.willmolloy.sync.Location;
import com.willmolloy.sync.util.docker.DockerHelper;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import jdk.incubator.concurrent.StructuredTaskScope;
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

      try (StructuredTaskScope.ShutdownOnFailure scope =
          new StructuredTaskScope.ShutdownOnFailure(
              "scan", Thread.ofVirtual().name("scan-worker-", 1).factory())) {
        // limit thread count; if I/O is slow (e.g. network drive), it creates too many threads at
        // once and grinds to a halt
        // alternatively could use RecursiveAction (which uses a fixed sized ForkJoinPool) but found
        // StructuredTaskScope (virtual threads) is faster
        Semaphore semaphore = new Semaphore(Runtime.getRuntime().availableProcessors());
        scope.fork(() -> new ParallelWalk(rootDir, consumer, scope, semaphore).walk());
        scope.joinUntil(Instant.now().plus(Duration.ofHours(1)));
        scope.throwIfFailed();
      }

      return builder.build();
    } catch (RuntimeException
        | IOException
        | ExecutionException
        | InterruptedException
        | TimeoutException e) {
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

  private record ParallelWalk(
      Path dir,
      BiConsumer<Path, BasicFileAttributes> consumer,
      StructuredTaskScope<Object> scope,
      Semaphore semaphore) {

    private Void walk() throws IOException {
      Files.walkFileTree(
          dir,
          new FileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attributes) {
              if (ParallelWalk.this.dir == dir) {
                log.debug("visit({})", dir);
                consumer.accept(dir, attributes);
                return FileVisitResult.CONTINUE;
              } else {
                // fork for each new directory discovered
                scope.fork(
                    () -> {
                      semaphore.acquire();
                      new ParallelWalk(dir, consumer, scope, semaphore).walk();
                      semaphore.release();
                      return null;
                    });
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
          });
      return null;
    }
  }
}
