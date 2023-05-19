package com.willmolloy.backup;

import static java.util.function.Predicate.not;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

/**
 * Represents a {@link Backup.Location}s file tree.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public final class FileTree {

  public static FileTree from(Map<Path, ? extends File> map) {
    return new FileTree(map);
  }

  private final TreeMap<Path, ? extends File> nodes;

  private FileTree(Map<Path, ? extends File> nodes) {
    this.nodes = new TreeMap<>(nodes);
  }

  void forEach(BiConsumer<? super Path, ? super File> consumer) {
    nodes.forEach(consumer);
  }

  Optional<File> get(Path key) {
    return Optional.ofNullable(nodes.get(key));
  }

  boolean contains(Path key) {
    return nodes.containsKey(key);
  }

  boolean containsParentOf(Path key) {
    Path parent = key.getParent();
    return parent != null && nodes.containsKey(parent);
  }

  boolean containsAnyChildOf(Path key) {
    Path nextKey = nodes.higherKey(key);
    return nextKey != null && nextKey.startsWith(key);
  }

  long fileCount() {
    return files().count();
  }

  long totalSize() {
    return files().mapToLong(File::size).sum();
  }

  private Stream<? extends File> files() {
    return nodes.values().stream().filter(not(File::isDirectory));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FileTree fileTree = (FileTree) o;
    return Objects.equals(nodes, fileTree.nodes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(nodes);
  }

  @Override
  public String toString() {
    return "%s[%s]".formatted(getClass().getSimpleName(), nodes);
  }

  /** Represents a file in the {@link FileTree}. */
  public interface File {

    /** File size in bytes. */
    long size();

    /** {@code true} if directory. {@code false} if regular file. */
    boolean isDirectory();

    /**
     * {@code true} if the {@code other} file can be considered the same.
     *
     * @apiNote Used to determine if a file requires updating.
     * @implNote The default implementation just looks at file size.
     */
    default boolean same(File other) {
      // for s3; considered last-modified, but it's really object-creation time.
      // also considered e-tag, but it's calculated differently for large (> 16MB) files.
      // file size is good enough?
      return isDirectory() == other.isDirectory() && size() == other.size();
    }
  }
}
