package com.willmolloy.sync.local;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.willmolloy.sync.FileTree;
import com.willmolloy.sync.Location;
import com.willmolloy.sync.util.docker.DockerHelper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
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

      try (StructuredTaskScope<Void> scope =
          new StructuredTaskScope<>(null, Thread.ofVirtual().name("scan-worker-", 1).factory())) {
        new ParallelWalk(rootDir, consumer, scope).walk();
        scope.join();
      } catch (InterruptedException e) {
        throw new IllegalStateException(e);
      }

      return builder.build();
    } catch (IOException e) {
      log.error("Error scanning: [{}]", this, e);
      throw new UncheckedIOException(e);
    }
  }

  public Path root() {
    return rootDir;
  }

  @Override
  public String toString() {
    return "%s[%s]".formatted(getClass().getSimpleName(), displayRootDir);
  }

  // ideas from:
  // https://gist.github.com/ryan-beckett/f298ab6fe84f3fb8025aa4cb28b8c793
  // https://github.com/javaparser/javaparser/blob/cfc1bcdf1fbd596ac11cbf14be565ffdee8903a5/javaparser-core/src/main/java/com/github/javaparser/utils/SourceRoot.java#L568
  // https://stackoverflow.com/questions/74487536/in-loom-can-i-use-virtual-threads-for-recursiveaction-task
  private record ParallelWalk(
      Path dir, BiConsumer<Path, BasicFileAttributes> consumer, StructuredTaskScope<Void> scope) {

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
                scope.fork(() -> new ParallelWalk(dir, consumer, scope).walk());
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
