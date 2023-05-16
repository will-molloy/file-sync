package com.willmolloy.backup;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

/**
 * Represents a locations file tree.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public final class FileTree {

  public static FileTree create(Map<Path, ? extends Node> map) {
    return new FileTree(map);
  }

  private final TreeMap<Path, ? extends Node> nodes;

  private FileTree(Map<Path, ? extends Node> nodes) {
    this.nodes = new TreeMap<>(nodes);
  }

  void forEach(BiConsumer<? super Path, ? super Node> consumer) {
    nodes.forEach(consumer);
  }

  Node get(Path key) {
    return nodes.get(key);
  }

  boolean contains(Path key) {
    return nodes.containsKey(key);
  }

  boolean containsParentOf(Path key) {
    Path parent = key.getParent();
    return parent != null && nodes.containsKey(parent);
  }

  // TODO unit tests
  boolean containsAnyChildOf(Path key) {
    // log(n) prefix check
    Path nextKey = nodes.higherKey(key);
    return nextKey != null && nextKey.startsWith(key);
  }

  long fileCount() {
    return files().count();
  }

  long totalSize() {
    return files().mapToLong(Node.File::size).sum();
  }

  private Stream<Node.File> files() {
    return nodes.values().stream()
        .flatMap(node -> node instanceof Node.File file ? Stream.of(file) : Stream.empty());
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
    return "FileTree[%s]".formatted(nodes);
  }

  /** Represents a node in a locations file tree. Either a file or directory. */
  public sealed interface Node permits Node.File, Node.Directory {

    /**
     * {@code true} if the {@code other} file can be considered the same.
     *
     * @apiNote Used to determine if a file requires updating.
     * @implNote The default implementation just looks at file size.
     */
    // TODO generify Node such that same is only called for same type. I.e. File vs File, Dir vs Dir
    // TODO just combine the classes again...?
    boolean same(Node other);

    /** File. */
    non-sealed interface File extends Node {

      /** File size in bytes. */
      long size();

      /**
       * {@code true} if the {@code other} file can be considered the same.
       *
       * @apiNote Used to determine if a file requires updating.
       * @implNote The default implementation just looks at file size.
       */
      // for s3; considered last-modified, but it's really object-creation time.
      // also considered e-tag, but it's calculated differently for large (> 16MB) files.
      // file size is good enough?
      @Override
      default boolean same(Node other) {
        return other instanceof File file && size() == file.size();
      }
    }

    /** Directory. */
    non-sealed interface Directory extends Node {

      @Override
      default boolean same(Node other) {
        return other instanceof Directory;
      }
    }
  }
}
