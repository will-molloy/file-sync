package com.willmolloy.backup;

import static com.willmolloy.backup.util.PathHelper.nameComponents;
import static java.util.Objects.requireNonNull;
import static java.util.function.Predicate.not;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nullable;
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

  TrieBasedFileTree(Trie<FileT> trie) {
    this.trie = requireNonNull(trie);
  }

  @Override
  public Optional<FileT> get(Path relativePath) {
    return trie.get(relativePath).map(Trie.Node::file);
  }

  @Override
  public Stream<FileT> preorder() {
    return trie.root.preorder().map(Trie.Node::file);
  }

  @Override
  public Stream<FileT> leaves() {
    return trie.root.preorder().filter(node -> node.children.isEmpty()).map(Trie.Node::file);
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
  public FileTree<FileT> subtree(FileT file) {
    return trie.get(file.relativePath())
        .map(node -> new TrieBasedFileTree<>(new Trie<>(node)))
        .orElseGet(() -> TrieBasedFileTree.<FileT>builder().build());
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

    private Trie(Node<FileT> root) {
      this.root = requireNonNull(root);
    }

    private void insert(FileT file, @Nullable Function<String, FileT> directoryFiller) {
      Node<FileT> node = root;
      StringBuilder pathSoFar = new StringBuilder();
      for (String c : nameComponents(file.relativePath())) {
        Node<FileT> child = node.children.get(c);
        pathSoFar.append(c);
        if (child == null) {
          if (directoryFiller == null) {
            child = new Node<>(null, node);
          } else {
            child = new Node<>(requireNonNull(directoryFiller.apply(pathSoFar.toString())), node);
          }
          node.children.put(c, child);
        }
        node = child;
        pathSoFar.append('/');
      }
      node.file = file;
    }

    private Optional<Node<FileT>> get(Path path) {
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
      @Nullable private FileT file;

      // trie fields
      @Nullable private final Node<FileT> parent;
      private final Map<String, Node<FileT>> children;

      private Node(FileT file, Node<FileT> parent) {
        this.file = file;
        this.parent = parent;
        this.children = new HashMap<>();
      }

      private Stream<Node<FileT>> preorder() {
        return Stream.concat(Stream.of(this), children.values().stream().flatMap(Node::preorder))
            .filter(Node::containsData);
      }

      private boolean containsData() {
        return file != null;
      }

      private FileT file() {
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
    @Nullable private final Function<String, FileT> directoryFiller;

    private Builder(Trie<FileT> trie, @Nullable Function<String, FileT> directoryFiller) {
      this.trie = requireNonNull(trie);
      this.directoryFiller = directoryFiller;
    }

    private Builder() {
      this(new Trie<>(new Trie.Node<>(null, null)), null);
    }

    @Override
    public Builder<FileT> withDirectoryFiller(Function<String, FileT> directoryFiller) {
      return new TrieBasedFileTree.Builder<>(
          new Trie<>(new Trie.Node<>(requireNonNull(directoryFiller.apply("")), null)),
          directoryFiller);
    }

    @Override
    public Builder<FileT> insert(FileT file) {
      EntryMessage m = log.traceEntry("insert({})", file);
      trie.insert(file, directoryFiller);
      return log.traceExit(m, this);
    }

    @Override
    public TrieBasedFileTree<FileT> build() {
      return new TrieBasedFileTree<>(trie);
    }
  }
}
