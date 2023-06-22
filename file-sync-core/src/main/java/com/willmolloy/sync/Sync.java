package com.willmolloy.sync;

/**
 * Sync type.
 *
 * @see BaseSync
 * @param <SourceFileT> source file type
 * @param <DestFileT> destination file type
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public interface Sync<SourceFileT extends File, DestFileT extends File> {

  Location<SourceFileT> source();

  Location<DestFileT> destination();

  boolean run();
}
