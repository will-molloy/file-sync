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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
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
  private final String displayRootDir;

  public LocalStorage(Path rootDir) {
    this.rootDir = checkNotNull(rootDir);
    checkArgument(Files.isDirectory(rootDir), "Requires a directory: [%s]", rootDir);
    DockerHelper docker = new DockerHelper();
    displayRootDir = docker.getHostPath(rootDir.toString()).orElse(rootDir.toString());
  }

  @Override
  public FileTree<LocalFile> scan() {
    try (ForkJoinPool pool = new ForkJoinPool()) {
      FileTree.Builder<LocalFile> builder = FileTree.builder(LocalFile.fromPath(this, rootDir));
      BiConsumer<Path, BasicFileAttributes> consumer =
          (path, attributes) -> {
            if (path == rootDir) {
              return;
            }
            LocalFile file = LocalFile.fromAttributes(this, path, attributes);
            builder.insert(file);
          };
      // TODO broken because FileTree.Builder assumes pre-order insert?
      DirectoryWalker walker = new DirectoryWalker(rootDir, consumer);
      pool.invoke(walker);
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
    return "%s[%s]".formatted(getClass().getSimpleName(), displayRootDir);
  }

  // credit: https://gist.github.com/ryan-beckett/f298ab6fe84f3fb8025aa4cb28b8c793
  // actually it's from:
  // https://github.com/javaparser/javaparser/blob/cfc1bcdf1fbd596ac11cbf14be565ffdee8903a5/javaparser-core/src/main/java/com/github/javaparser/utils/SourceRoot.java#L568
  private static final class DirectoryWalker extends RecursiveAction {
    private final Path dir;
    private final BiConsumer<Path, BasicFileAttributes> consumer;

    private DirectoryWalker(Path dir, BiConsumer<Path, BasicFileAttributes> consumer) {
      this.dir = dir;
      this.consumer = consumer;
    }

    @Override
    protected void compute() {
      List<DirectoryWalker> walks = new ArrayList<>();
      try {
        Files.walkFileTree(
            dir,
            new FileVisitor<>() {
              @Override
              public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attributes) {
                if (DirectoryWalker.this.dir == dir) {
                  consumer.accept(dir, attributes);
                  return FileVisitResult.CONTINUE;
                } else {
                  // new thread for each new directory discovered
                  DirectoryWalker w = new DirectoryWalker(dir, consumer);
                  w.fork();
                  walks.add(w);
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
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }

      for (DirectoryWalker w : walks) {
        w.join();
      }
    }
  }
}
