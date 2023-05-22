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
 * @param <FileT> type of file stored in this file tree
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
final class TrieBasedFileTree<FileT extends File> implements FileTree<FileT> {

  static <FileT extends File> TrieBasedFileTree<FileT> from(Set<FileT> set) {
    Trie<FileT> trie = new Trie<>();
    set.forEach(trie::insert);
    return new TrieBasedFileTree<>(trie);
  }

  private final Trie<FileT> trie;

  private TrieBasedFileTree(Trie<FileT> trie) {
    this.trie = requireNonNull(trie);
  }

  @Override
  public void forEach(Consumer<FileT> consumer) {
    trie.root.stream().map(Trie.Node::file).forEach(consumer);
  }

  @Override
  public Optional<FileT> get(Path relativePath) {
    return trie.get(relativePath).map(Trie.Node::file);
  }

  @Override
  public boolean contains(Path relativePath) {
    return trie.get(relativePath).isPresent();
  }

  @Override
  public Stream<FileT> ancestors(Path relativePath) {
    return trie.get(relativePath).stream()
        .flatMap(node -> Stream.iterate(node.parent, parent -> parent.parent))
        .takeWhile(node -> node != trie.root)
        .filter(Trie.Node::containsData)
        .map(Trie.Node::file);
  }

  @Override
  public Stream<FileT> descendants(Path relativePath) {
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
    if (o instanceof TrieBasedFileTree<?> fileTree) {
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

  /**
   * Trie over {@link Path} {@linkplain Path#iterator name components}.
   *
   * @param <FileT> type of file stored in the trie nodes
   */
  private static final class Trie<FileT extends File> {
    private final Node<FileT> root = new Node<>(null);

    void insert(FileT file) {
      Node<FileT> node = root;
      for (Path c : file.relativePath()) {
        Node<FileT> child = node.children.get(c);
        if (child == null) {
          child = new Node<>(node);
          node.children.put(c, child);
        }
        node = child;
      }
      node.file = file;
    }

    Optional<Node<FileT>> get(Path path) {
      Node<FileT> node = root;
      for (Path c : path) {
        Node<FileT> child = node.children.get(c);
        if (child == null) {
          return Optional.empty();
        }
        node = child;
      }
      return node.containsData() ? Optional.of(node) : Optional.empty();
    }

    /**
     * {@link Trie} node.
     *
     * @param <FileT> type of file stored in this node
     */
    private static final class Node<FileT extends File> {
      // non-null if the node represents a file
      // null if the node exists only for trie traversal
      private FileT file;

      // trie fields
      private final Node<FileT> parent;
      private final HashMap<Path, Node<FileT>> children;

      Node(Node<FileT> parent) {
        this.parent = parent;
        this.children = new HashMap<>();
      }

      Stream<Node<FileT>> stream() {
        return Stream.concat(Stream.of(this), children.values().stream().flatMap(Node::stream))
            .filter(Node::containsData);
      }

      // TODO remove, fill with dummy 'directory' nodes
      boolean containsData() {
        return file != null;
      }

      FileT file() {
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
        if (o instanceof Node<?> node) {
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
