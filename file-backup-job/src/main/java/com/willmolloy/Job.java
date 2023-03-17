package com.willmolloy;

import java.util.List;

/**
 * Job definition.
 *
 * @param <SourceT> source file type.
 * @param <DestinationT> destination file type.
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public interface Job<SourceT, DestinationT> {

  List<SourceT> scanSource();

  List<DestinationT> scanDestination();

  void copyToDestination(SourceT sourceFile, DestinationT destinationFile);

  void deleteFromDestination(DestinationT destinationFile);
}
