package com.willmolloy.backup;

import static java.util.Objects.requireNonNull;
import static java.util.function.Predicate.not;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * {@link FileTree} implemented via {@linkplain Trie trie data structure}.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
final class TrieBasedFileTree implements FileTree {

  static TrieBasedFileTree from(Set<? extends File> set) {
    Trie trie = new Trie();
    set.forEach(trie::insert);
    return new TrieBasedFileTree(trie);
  }

  private final Trie trie;

  private TrieBasedFileTree(Trie trie) {
    this.trie = requireNonNull(trie);
  }

  @Override
  public void forEach(Consumer<File> consumer) {
    trie.root.stream().map(Trie.Node::file).forEach(consumer);
  }

  @Override
  public Optional<File> get(Path relativePath) {
    return trie.get(relativePath).map(Trie.Node::file);
  }

  @Override
  public boolean contains(Path relativePath) {
    return trie.get(relativePath).isPresent();
  }

  @Override
  public Stream<File> ancestors(Path relativePath) {
    return trie.get(relativePath).stream()
        .flatMap(node -> Stream.iterate(node.parent, parent -> parent.parent))
        .takeWhile(node -> node != trie.root)
        .filter(Trie.Node::containsData)
        .map(Trie.Node::file);
  }

  @Override
  public Stream<File> descendants(Path relativePath) {
    return trie.get(relativePath).stream().flatMap(Trie.Node::stream).skip(1).map(Trie.Node::file);
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
    return trie.root.stream().map(Trie.Node::file).filter(not(File::isDirectory));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null) {
      return false;
    }
    if (o instanceof TrieBasedFileTree fileTree) {
      return Objects.equals(trie.root, fileTree.trie.root);
    }
    return false;
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

    void insert(File file) {
      Node node = root;
      for (Path c : file.relativePath()) {
        Node child = node.children.get(c);
        if (child == null) {
          child = new Node(node);
          node.children.put(c, child);
        }
        node = child;
      }
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
      return node.containsData() ? Optional.of(node) : Optional.empty();
    }

    /** {@link Trie} node. */
    private static final class Node {
      // non-null if the node represents a file
      // null if the node exists only for trie traversal
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
            .filter(Node::containsData);
      }

      // TODO remove, fill with dummy 'directory' nodes
      boolean containsData() {
        return file != null;
      }

      File file() {
        return file;
      }

      @Override
      public boolean equals(Object o) {
        if (this == o) {
          return true;
        }
        if (o == null) {
          return false;
        }
        if (o instanceof Node node) {
          return Objects.equals(file, node.file) && Objects.equals(children, node.children);
        }
        return false;
      }

      @Override
      public int hashCode() {
        return Objects.hash(file, children);
      }

      @Override
      public String toString() {
        return "Node[file=%s, children=%s]".formatted(file, children);
      }
    }
  }
}
