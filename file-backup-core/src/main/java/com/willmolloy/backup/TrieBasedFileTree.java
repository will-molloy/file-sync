package com.willmolloy.backup;

import static java.util.Objects.requireNonNull;
import static java.util.function.Predicate.not;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

/**
 * {@link FileTree} implemented via {@linkplain Trie trie data structure}.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
final class TrieBasedFileTree implements FileTree {

  static TrieBasedFileTree from(Map<Path, ? extends File> map) {
    Trie trie = new Trie();
    map.forEach(trie::insert);
    return new TrieBasedFileTree(trie);
  }

  private final Trie trie;

  private TrieBasedFileTree(Trie trie) {
    this.trie = requireNonNull(trie);
  }

  @Override
  public void forEach(BiConsumer<Path, File> consumer) {
    trie.root.stream().forEach(node -> consumer.accept(node.path, node.file));
  }

  @Override
  public Optional<File> get(Path key) {
    return trie.get(key).map(node -> node.file);
  }

  @Override
  public boolean contains(Path key) {
    return trie.get(key).isPresent();
  }

  @Override
  public Stream<Path> ancestors(Path key) {
    return trie.get(key).stream()
        .flatMap(node -> Stream.iterate(node.parent, parent -> parent.parent))
        .takeWhile(node -> node != trie.root)
        .filter(node -> node.path != null)
        .map(node -> node.path);
  }

  @Override
  public Stream<Path> descendants(Path key) {
    return trie.get(key).stream().flatMap(Trie.Node::stream).skip(1).map(node -> node.path);
  }

  @Override
  public long fileCount() {
    return files().count();
  }

  @Override
  public long totalSize() {
    return files().mapToLong(File::size).sum();
  }

  private Stream<? extends File> files() {
    return trie.root.stream().map(node -> node.file).filter(not(File::isDirectory));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TrieBasedFileTree fileTree = (TrieBasedFileTree) o;
    return Objects.equals(trie.root, fileTree.trie.root);
  }

  @Override
  public int hashCode() {
    return Objects.hash(trie.root);
  }

  @Override
  public String toString() {
    return "FileTree[root=%s]".formatted(trie.root);
  }

  /** Trie over {@link Path} {@linkplain Path#iterator name components}. */
  private static final class Trie {
    private final Node root = new Node(null);

    void insert(Path path, File file) {
      Node node = root;
      for (Path c : path) {
        Node child = node.children.get(c);
        if (child == null) {
          child = new Node(node);
          node.children.put(c, child);
        }
        node = child;
      }
      node.path = path;
      node.file = file;
    }

    Optional<Node> get(Path path) {
      Node node = root;
      for (Path c : path) {
        Node child = node.children.get(c);
        if (child == null) {
          return Optional.empty();
        }
        node = child;
      }
      return node.path != null ? Optional.of(node) : Optional.empty();
    }

    /** {@link Trie} node. */
    private static final class Node {
      // data fields
      // non-null if the node represents a file
      // null if the node exists only for trie traversal
      private Path path;
      private File file;

      // trie fields
      private final Node parent;
      private final HashMap<Path, Node> children;

      Node(Node parent) {
        this.parent = parent;
        this.children = new HashMap<>();
      }

      Stream<Node> stream() {
        return Stream.concat(Stream.of(this), children.values().stream().flatMap(Node::stream))
            .filter(node -> node.path != null);
      }

      @Override
      public boolean equals(Object o) {
        if (this == o) {
          return true;
        }
        if (o == null || getClass() != o.getClass()) {
          return false;
        }
        Node node = (Node) o;
        return Objects.equals(path, node.path) && Objects.equals(children, node.children);
      }

      @Override
      public int hashCode() {
        return Objects.hash(path, children);
      }

      @Override
      public String toString() {
        return "Node[path=%s, children=%s]".formatted(path, children);
      }
    }
  }
}
