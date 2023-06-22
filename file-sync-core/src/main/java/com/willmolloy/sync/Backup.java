package com.willmolloy.sync;

/**
 * Backup type.
 *
 * @see BaseBackup
 * @param <SourceFileT> source file type
 * @param <DestFileT> destination file type
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public interface Backup<SourceFileT extends File, DestFileT extends File> {

  Location<SourceFileT> source();

  Location<DestFileT> destination();

  boolean run();
}
