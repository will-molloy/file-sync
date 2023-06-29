package com.willmolloy.sync;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.willmolloy.sync.util.PathHelper.nameComponents;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * {@link FileTree} implemented like an n-ary tree/trie over {@link Path} {@linkplain Path#iterator
 * name components}.
 *
 * @param <FileT> type of file stored in this file tree
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
final class FileTreeNode<FileT extends File> implements FileTree<FileT> {

  private static final Logger log = LogManager.getLogger();

  private final FileT file;
  @Nullable private final FileTreeNode<FileT> parent; // only null for root node
  private final ConcurrentMap<String, FileTreeNode<FileT>> children;

  private FileTreeNode(FileT file, FileTreeNode<FileT> parent) {
    this.file = checkNotNull(file);
    this.parent = parent;
    this.children = new ConcurrentHashMap<>();
  }

  @Override
  public FileT root() {
    return file;
  }

  @Override
  public Optional<FileT> correspondent(File file) {
    return getNode(file.relativePath()).map(getFile);
  }

  @Override
  public Stream<FileT> postorder() {
    return postorderNodes().map(getFile);
  }

  @Override
  public Stream<FileT> leaves() {
    return postorderNodes().filter(node -> node.children.isEmpty()).map(getFile);
  }

  @Override
  public Stream<FileT> ancestors(FileT file) {
    return getNode(file.relativePath()).stream()
        .flatMap(node -> Stream.iterate(node.parent, parent -> parent.parent))
        .takeWhile(node -> node != this.parent)
        .map(getFile);
  }

  @Override
  public FileTree<FileT> subtree(FileT file) {
    return getNode(file.relativePath())
        // shouldn't get here... only if you did subtree.subtree
        .orElseThrow(IllegalArgumentException::new);
  }

  @Override
  public long leafCount() {
    return leaves().count();
  }

  @Override
  public long totalSize() {
    return leaves().mapToLong(File::size).sum();
  }

  private Optional<FileTreeNode<FileT>> getNode(Path path) {
    FileTreeNode<FileT> node = this;
    List<String> pathToNode = nameComponents(this.file.relativePath().relativize(path));
    for (String c : pathToNode) {
      FileTreeNode<FileT> child = node.children.get(c);
      if (child == null) {
        return Optional.empty();
      }
      node = child;
    }
    return Optional.of(node);
  }

  // multithreaded insert is safe:
  // really only care about leaves; they'll never be overwritten as there's only one!
  // (non-empty) directories; may be race where they're overwritten by directory filler, but filler
  // should provide the same behaviour
  private void insertNode(FileT file, DirectoryFiller<FileT> directoryFiller) {
    FileTreeNode<FileT> node = this;
    StringBuilder pathSoFar = new StringBuilder(this.file.relativePath().toString());
    List<String> pathToNode =
        nameComponents(this.file.relativePath().relativize(file.relativePath()));
    for (String c : pathToNode.subList(0, pathToNode.size() - 1)) {
      pathSoFar.append(c);
      FileTreeNode<FileT> parent = node;
      node =
          node.children.computeIfAbsent(
              c, k -> new FileTreeNode<>(directoryFiller.apply(pathSoFar.toString()), parent));
      pathSoFar.append('/');
    }
    // TODO SequencedCollection#getLast
    node.children.put(pathToNode.get(pathToNode.size() - 1), new FileTreeNode<>(file, node));
  }

  private Stream<FileTreeNode<FileT>> postorderNodes() {
    return Stream.concat(
        children.values().stream().flatMap(FileTreeNode::postorderNodes), Stream.of(this));
  }

  private final Function<FileTreeNode<FileT>, FileT> getFile = node -> node.file;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    return o instanceof FileTreeNode<?> node
        && Objects.equals(file, node.file)
        && Objects.equals(children, node.children);
  }

  @Override
  public int hashCode() {
    return Objects.hash(file, children);
  }

  @Override
  public String toString() {
    return "Node[file=%s, children=%s]".formatted(file, children);
  }

  /**
   * {@link FileTree.Builder} implementation for {@link FileTreeNode}.
   *
   * @param <FileT> type of file stored in the built file tree
   */
  static final class Builder<FileT extends File> implements FileTree.Builder<FileT> {
    private final FileTreeNode<FileT> root;
    private final DirectoryFiller<FileT> directoryFiller;

    Builder(FileT root, DirectoryFiller<FileT> directoryFiller) {
      this.root = new FileTreeNode<>(root, null);
      this.directoryFiller = checkNotNull(directoryFiller);
    }

    @Override
    public Builder<FileT> insert(FileT file) {
      log.debug("insert({})", file);
      root.insertNode(file, directoryFiller);
      return this;
    }

    @Override
    public FileTreeNode<FileT> build() {
      return root;
    }
  }
}
