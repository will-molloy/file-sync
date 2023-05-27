package com.willmolloy.backup;

import static com.willmolloy.backup.util.PathHelper.nameComponents;
import static java.util.Objects.requireNonNull;
import static java.util.function.Predicate.not;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.EntryMessage;

/**
 * {@link FileTree} implemented via {@linkplain Trie trie data structure}.
 *
 * @param <FileT> type of file stored in this file tree
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
final class TrieBasedFileTree<FileT extends File> implements FileTree<FileT> {

  private static final Logger log = LogManager.getLogger();

  private final Trie<FileT> trie;

  private TrieBasedFileTree(Trie<FileT> trie) {
    this.trie = requireNonNull(trie);
  }

  @Override
  public Stream<FileT> preorder() {
    return trie.root.stream().map(Trie.Node::file);
  }

  @Override
  public Stream<FileT> leaves() {
    return preorder().filter(this::isLeaf);
  }

  private boolean isLeaf(FileT file) {
    return descendants(file).noneMatch(e -> true);
  }

  @Override
  public Optional<FileT> get(Path relativePath) {
    return trie.get(relativePath).map(Trie.Node::file);
  }

  @Override
  public Stream<FileT> ancestors(FileT file) {
    return trie.get(file.relativePath()).stream()
        .flatMap(node -> Stream.iterate(node.parent, parent -> parent.parent))
        .takeWhile(node -> node != trie.root.parent)
        .filter(Trie.Node::containsData)
        .map(Trie.Node::file);
  }

  @Override
  public Stream<FileT> descendants(FileT file) {
    return trie.get(file.relativePath()).stream()
        .flatMap(Trie.Node::stream)
        .skip(1)
        .map(Trie.Node::file);
  }

  @Override
  public long fileCount() {
    return files().count();
  }

  @Override
  public long totalSize() {
    return files().mapToLong(File::size).sum();
  }

  private Stream<FileT> files() {
    return preorder().filter(not(File::isDirectory));
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
    private final Node<FileT> root;
    private final Function<String, FileT> directoryFiller;

    Trie(Function<String, FileT> directoryFiller) {
      this.directoryFiller = directoryFiller;
      if (directoryFiller == null) {
        this.root = new Node<>(null, null);
      } else {
        this.root = new Node<>(null, requireNonNull(directoryFiller.apply("")));
      }
    }

    void insert(FileT file) {
      Node<FileT> node = root;
      StringBuilder path = new StringBuilder();
      for (String c : nameComponents(file.relativePath())) {
        Node<FileT> child = node.children.get(c);
        path.append(c);
        if (child == null) {
          if (directoryFiller == null) {
            child = new Node<>(node, null);
          } else {
            child = new Node<>(node, requireNonNull(directoryFiller.apply(path.toString())));
          }
          node.children.put(c, child);
        }
        node = child;
        path.append('/');
      }
      node.file = file;
    }

    Optional<Node<FileT>> get(Path path) {
      Node<FileT> node = root;
      for (String c : nameComponents(path)) {
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
      private FileT file;

      // trie fields
      private final Node<FileT> parent;
      private final HashMap<String, Node<FileT>> children;

      Node(Node<FileT> parent, FileT file) {
        this.parent = parent;
        this.children = new HashMap<>();
        this.file = file;
      }

      Stream<Node<FileT>> stream() {
        return Stream.concat(Stream.of(this), children.values().stream().flatMap(Node::stream))
            .filter(Node::containsData);
      }

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

  static <FileT extends File> TrieBasedFileTree.Builder<FileT> builder() {
    return new TrieBasedFileTree.Builder<>();
  }

  /**
   * {@link FileTree.Builder} implementation for {@link TrieBasedFileTree}.
   *
   * @param <FileT> type of file stored in the built file tree
   */
  static final class Builder<FileT extends File> implements FileTree.Builder<FileT> {

    private final Trie<FileT> trie;

    private Builder(Trie<FileT> trie) {
      this.trie = requireNonNull(trie);
    }

    private Builder() {
      this(new Trie<>(null));
    }

    @Override
    public Builder<FileT> withDirectoryFiller(Function<String, FileT> directoryFiller) {
      return new TrieBasedFileTree.Builder<>(new Trie<>(directoryFiller));
    }

    @Override
    public Builder<FileT> insert(FileT file) {
      EntryMessage m = log.traceEntry("insert({})", file);
      trie.insert(file);
      return log.traceExit(m, this);
    }

    @Override
    public TrieBasedFileTree<FileT> build() {
      return new TrieBasedFileTree<>(trie);
    }
  }
}
