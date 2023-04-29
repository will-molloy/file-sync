package com.willmolloy.backup;

import java.nio.file.Path;
import java.util.Map;

/**
 * Represents a file tree, built like a trie.
 *
 * @param root root node
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
record FileTree(Node root) {
  FileTree(Map<Path, Node> children) {
    this(new Node(children));
  }

  FileTree() {
    this(Map.of());
  }

  FileTree difference(FileTree other) {
    return new FileTree();
  }

  FileTree intersection(FileTree other) {
    return new FileTree();
  }

  /**
   * File tree node.
   *
   * @param children child nodes
   */
  record Node(Map<Path, Node> children) {
    Node(Map<Path, Node> children) {
      this.children = Map.copyOf(children);
    }

    Node() {
      this(Map.of());
    }

    long count(){
      return children.size() + children.values().stream().mapToLong(n -> n.count()).sum();
    }
  }
}
