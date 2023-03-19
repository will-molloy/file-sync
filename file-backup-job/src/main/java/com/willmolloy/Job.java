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

  void copyToDestination(Path file);

  void deleteFromDestination(Path file);

  boolean isNewerOnSource(Path file);
}
