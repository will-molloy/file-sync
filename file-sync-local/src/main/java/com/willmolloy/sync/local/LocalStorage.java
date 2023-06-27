package com.willmolloy.sync.local;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Throwables;
import com.willmolloy.sync.FileTree;
import com.willmolloy.sync.Location;
import com.willmolloy.sync.util.docker.DockerHelper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.ForkJoinWorkerThread;
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

      try (ForkJoinPool forkJoinPool = forkJoinPool()) {
        DirectoryWalker directoryWalker = new DirectoryWalker(rootDir, consumer);
        forkJoinPool.invoke(directoryWalker);
      }

      return builder.build();
    } catch (RuntimeException | IOException e) {
      log.error("Error scanning: [{}]", this, e);
      Throwables.throwIfUnchecked(e);
      throw new RuntimeException(e);
    }
  }

  private static ForkJoinPool forkJoinPool() {
    ForkJoinPool.ForkJoinWorkerThreadFactory factory =
        pool -> {
          ForkJoinWorkerThread worker =
              ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
          worker.setName("scan-worker-%d".formatted(worker.getPoolIndex()));
          return worker;
        };
    return new ForkJoinPool(Runtime.getRuntime().availableProcessors() * 2, factory, null, false);
  }

  public Path root() {
    return rootDir;
  }

  @Override
  public String toString() {
    return "%s[%s]".formatted(getClass().getSimpleName(), displayRootDir);
  }

  @SuppressFBWarnings(value = {"SE_BAD_FIELD", "SE_NO_SERIALVERSIONID"})
  private static final class DirectoryWalker extends RecursiveAction {
    private final Path rootDir;
    private final BiConsumer<Path, BasicFileAttributes> consumer;

    private DirectoryWalker(Path rootDir, BiConsumer<Path, BasicFileAttributes> consumer) {
      this.rootDir = rootDir;
      this.consumer = consumer;
    }

    @Override
    protected void compute() {
      List<ForkJoinTask<Void>> forks = new ArrayList<>();
      try {
        Files.walkFileTree(
            rootDir,
            new FileVisitor<>() {
              @Override
              public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attributes) {
                if (rootDir == dir) {
                  consumer.accept(dir, attributes);
                  return FileVisitResult.CONTINUE;
                } else {
                  // fork for each new directory discovered
                  ForkJoinTask<Void> fork = new DirectoryWalker(dir, consumer).fork();
                  forks.add(fork);
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
      for (ForkJoinTask<Void> fork : forks) {
        fork.join();
      }
    }
  }
}
