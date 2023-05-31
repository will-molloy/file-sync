package com.willmolloy.backup;

import static java.util.Objects.requireNonNull;

/**
 * Base {@link Backup} class with common methods implemented for convenience.
 *
 * @param <SourceFileT> source file type
 * @param <DestFileT> destination file type
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public abstract class BaseBackup<SourceFileT extends File, DestFileT extends File>
    implements Backup<SourceFileT, DestFileT> {

  private final Location<SourceFileT> source;
  private final Location<DestFileT> destination;

  protected BaseBackup(Location<SourceFileT> source, Location<DestFileT> destination) {
    this.source = requireNonNull(source);
    this.destination = requireNonNull(destination);
  }

  @Override
  public final Location<SourceFileT> source() {
    return source;
  }

  @Override
  public final Location<DestFileT> destination() {
    return destination;
  }

  @Override
  public final String toString() {
    return "%s[source=%s, destination=%s]"
        .formatted(getClass().getSimpleName(), source, destination);
  }
}
