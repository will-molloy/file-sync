package com.willmolloy.backup;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.willmolloy.backup.util.PathHelper.nameComponents;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import com.google.common.collect.Streams;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.MutableGraph;
import com.google.common.graph.Traverser;
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

  private final FileT root;
  private final ImmutableGraph<FileT> graph;

  private FileTreeNode(FileT root, ImmutableGraph<FileT> graph) {
    checkArgument(graph.isDirected());
    this.root = checkNotNull(root);
    this.graph = checkNotNull(graph);
  }

  @Override
  public FileT root() {
    return root;
  }

  @Override
  public Optional<FileT> correspondent(File file) {
    return null;
  }

  @Override
  public Stream<FileT> postorder() {
    return Streams.stream(Traverser.forTree(graph).depthFirstPostOrder(root));
  }

  @Override
  public Stream<FileT> leaves() {
    return graph.nodes().stream().filter(node -> graph.outDegree(node) == 0);
  }

  @Override
  public Stream<FileT> ancestors(FileT file) {
    return graph.predecessors(file).stream()
        .flatMap(this::ancestors)
        .takeWhile(node -> node != this.root);
  }

  @Override
  public FileTree<FileT> subtree(FileT file) {
    FileTree.Builder<FileT> builder = FileTree.builder(file);
    Streams.stream(Traverser.forTree(graph).depthFirstPreOrder(file)).forEach(builder::insert);
    return builder.build();
  }

  @Override
  public long leafCount() {
    return leaves().count();
  }

  @Override
  public long totalSize() {
    return leaves().mapToLong(File::size).sum();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    return o instanceof FileTreeNode<?> node
        && Objects.equals(root, node.root)
        && Objects.equals(graph, node.graph);
  }

  @Override
  public int hashCode() {
    return Objects.hash(root, graph);
  }

  @Override
  public String toString() {
    return "FileTree[%s]".formatted(graph);
  }

  /**
   * {@link FileTree.Builder} implementation for {@link FileTreeNode}.
   *
   * @param <FileT> type of file stored in the built file tree
   */
  static final class Builder<FileT extends File> implements FileTree.Builder<FileT> {
    private final FileT root;
    private final DirectoryFiller<FileT> directoryFiller;
    private final ImmutableGraph.Builder<FileT> graphBuilder;

    Builder(FileT root, DirectoryFiller<FileT> directoryFiller) {
      this.root = checkNotNull(root);
      this.directoryFiller = checkNotNull(directoryFiller);
      this.graphBuilder = GraphBuilder.directed().immutable();
      graphBuilder.addNode(root);
    }

    @Override
    public Builder<FileT> insert(FileT file) {
      log.debug("insert({})", file);

      FileT node = root;

      StringBuilder pathSoFar = new StringBuilder(root.relativePath().toString());
      List<String> pathToNode = nameComponents(root.relativePath().relativize(file.relativePath()));

      for (int i = 0; i < pathToNode.size(); i++) {
        pathSoFar.append(pathToNode.get(i));

        boolean last = i == pathToNode.size() - 1;

        if (last){
          graphBuilder.putEdge(node, file);
        } else {
          FileT next = directoryFiller.apply(pathSoFar.toString());
          // putEdge doesn't override existing connection
            graphBuilder.putEdge(node, next);
          node = next;
          pathSoFar.append('/');
        }
      }

      return this;
    }

    @Override
    public FileTreeNode<FileT> build() {
      return new FileTreeNode<>(root, graphBuilder.build());
    }
  }
}
