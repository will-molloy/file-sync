package com.willmolloy.backup;

import static com.willmolloy.backup.util.PathHelper.nameComponents;
import static java.util.Objects.requireNonNull;
import static java.util.function.Predicate.not;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * {@link FileTree} implemented like a trie over {@link Path} {@linkplain Path#iterator name components}.
 *
 * @param <FileT> type of file stored in this file tree
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
final class TrieLikeFileTree<FileT extends File> implements FileTree<FileT> {

  private static final Logger log = LogManager.getLogger();

  private final Node<FileT> root;

  private TrieLikeFileTree(Node<FileT> trie) {
    this.root = requireNonNull(trie);
  }

  @Override
  public Optional<FileT> get(Path relativePath) {
    return root.get(relativePath).map(Node::file);
  }

  @Override
  public Stream<FileT> postorder() {
    return root.postorder().map(Node::file);
  }

  @Override
  public Stream<FileT> leaves() {
    return root.postorder().filter(node -> node.children.isEmpty()).map(Node::file);
  }

  @Override
  public Stream<FileT> ancestors(FileT file) {
    return root.get(file.relativePath()).stream()
        .flatMap(node -> Stream.iterate(node.parent, parent -> parent.parent))
        .takeWhile(node -> node != root.parent)
        .map(Node::file);
  }

  @Override
  public FileTree<FileT> subtree(FileT file) {
    return root.get(file.relativePath())
        .map(TrieLikeFileTree::new)
        // shouldn't get here... only if you did subtree.subtree
        .orElseThrow(IllegalArgumentException::new);
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
    if (o instanceof TrieLikeFileTree<?> fileTree) {
      return Objects.equals(root, fileTree.root);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(root);
  }

  @Override
  public String toString() {
    return "FileTree[root=%s]".formatted(root);
  }

  /**
   * Represents a node in the file tree.
   *
   * @param <FileT> type of file stored in this node
   */
  private static final class Node<FileT extends File> {
    private final FileT file;
    @Nullable private final Node<FileT> parent;
    private final Map<String, Node<FileT>> children;

    private Node(FileT file, Node<FileT> parent) {
      this.file = requireNonNull(file);
      this.parent = parent;
      this.children = new LinkedHashMap<>();
    }

    private void insert(FileT file, DirectoryFiller<FileT> directoryFiller) {
      Node<FileT> root = this;
      Node<FileT> node = root;
      StringBuilder pathSoFar = new StringBuilder(root.file.relativePath().toString());
      List<String> pathToNode =
          nameComponents(root.file.relativePath().relativize(file.relativePath()));
      for (int i = 0; i < pathToNode.size(); i++) {
        pathSoFar.append(pathToNode.get(i));
        Node<FileT> currentNode = node;
        boolean last = i == pathToNode.size() - 1;
        node =
            node.children.computeIfAbsent(
                pathToNode.get(i),
                k ->
                    last
                        ? new Node<>(file, currentNode)
                        : new Node<>(directoryFiller.apply(pathSoFar.toString()), currentNode));
        pathSoFar.append('/');
      }
      // no need to set Node.file here; assuming only leaves are inserted or parent dirs are
      // inserted first (i.e. in a pre-order manner)
    }

    private Optional<Node<FileT>> get(Path path) {
      Node<FileT> root = this;
      Node<FileT> node = root;
      List<String> pathToNode = nameComponents(root.file.relativePath().relativize(path));
      for (String c : pathToNode) {
        Node<FileT> child = node.children.get(c);
        if (child == null) {
          return Optional.empty();
        }
        node = child;
      }
      return Optional.of(node);
    }

    private Stream<Node<FileT>> postorder() {
      return Stream.concat(children.values().stream().flatMap(Node::postorder), Stream.of(this));
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

  /**
   * {@link FileTree.Builder} implementation for {@link TrieLikeFileTree}.
   *
   * @param <FileT> type of file stored in the built file tree
   */
  static final class Builder<FileT extends File> implements FileTree.Builder<FileT> {
    private final Node<FileT> root;
    private final DirectoryFiller<FileT> directoryFiller;

    Builder(FileT root, DirectoryFiller<FileT> directoryFiller) {
      this.root = new Node<>(root, null);
      this.directoryFiller = requireNonNull(directoryFiller);
    }

    @Override
    public Builder<FileT> insert(FileT file) {
      log.debug("insert({})", file);
      root.insert(file, directoryFiller);
      return this;
    }

    @Override
    public TrieLikeFileTree<FileT> build() {
      return new TrieLikeFileTree<>(root);
    }
  }
}
