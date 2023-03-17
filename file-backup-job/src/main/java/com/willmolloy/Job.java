package com.willmolloy;

import java.nio.file.Path;
import java.util.List;

/**
 * Job definition.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public interface Job {

  Path sourceRoot();

  Path destinationRoot();

  List<Path> scanSource();

  List<Path> scanDestination();

  void copyToDestination(Path sourceFile, Path destinationLocation);

  void deleteFromDestination(Path destinationFile);
}
