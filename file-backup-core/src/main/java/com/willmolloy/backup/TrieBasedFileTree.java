package com.willmolloy.backup;

import static com.willmolloy.backup.util.PathHelper.nameComponents;
import static java.util.Objects.requireNonNull;
import static java.util.function.Predicate.not;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
  public Stream<FileT> postorder() {
    return trie.root.postorder().map(Trie.Node::file);
  }

  @Override
  public Stream<FileT> leaves() {
    return trie.root.postorder().filter(node -> node.children.isEmpty()).map(Trie.Node::file);
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
        .orElseThrow();
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
    return postorder().filter(not(File::isDirectory));
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

    private void insert(FileT file, Function<String, FileT> directoryFiller) {
      Node<FileT> node = root;
      // TODO broken when inserting into subtree... but that isn't used
      StringBuilder pathSoFar = new StringBuilder();
      List<String> pathToNode = nameComponents(file.relativePath());
      for (int i = 0; i < pathToNode.size(); i++) {
        pathSoFar.append(pathToNode.get(i));
        Node<FileT> currentNode = node;
        boolean last = i == pathToNode.size() - 1;
        node =
            node.children.computeIfAbsent(
                pathToNode.get(i),
                k -> {
                  if (last) {
                    return new Node<>(file, currentNode);
                  } else {
                    return new Node<>(directoryFiller.apply(pathSoFar.toString()), currentNode);
                  }
                });
        pathSoFar.append('/');
      }
      node.file = file;
    }

    private Optional<Node<FileT>> get(Path path) {
      Node<FileT> node = root;
      List<String> pathToNode =
          root.file == null
              ? nameComponents(path)
              : nameComponents(root.file.relativePath().relativize(path));
      for (String c : pathToNode) {
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
      @Nullable private final Node<FileT> parent;
      private final Map<String, Node<FileT>> children;

      private Node(FileT file, Node<FileT> parent) {
        this.file = requireNonNull(file);
        this.parent = parent;
        this.children = new HashMap<>();
      }

      private Stream<Node<FileT>> postorder() {
        return Stream.concat(children.values().stream().flatMap(Node::postorder), Stream.of(this))
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

  /**
   * {@link FileTree.Builder} implementation for {@link TrieBasedFileTree}.
   *
   * @param <FileT> type of file stored in the built file tree
   */
  static final class Builder<FileT extends File> implements FileTree.Builder<FileT> {
    private final Trie<FileT> trie;
    private final Function<String, FileT> directoryFiller;

    Builder(FileT root, Function<String, FileT> directoryFiller) {
      this.trie = new Trie<>(new Trie.Node<>(root, null));
      this.directoryFiller = requireNonNull(directoryFiller);
    }

    @Override
    public Builder<FileT> insert(FileT file) {
      log.debug("insert({})", file);
      trie.insert(file, directoryFiller);
      return this;
    }

    @Override
    public TrieBasedFileTree<FileT> build() {
      // TODO post condition?
      return new TrieBasedFileTree<>(trie);
    }
  }
}
