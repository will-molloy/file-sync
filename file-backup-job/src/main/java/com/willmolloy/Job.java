package com.willmolloy;

import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Job definition.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public interface Job {

  Stream<Path> scanSource();

  Stream<Path> scanDestination();

  void copy(Path file);

  void delete(Path file);

  void update(Path file);

  boolean sourceNotEqualDestination(Path file);
}
