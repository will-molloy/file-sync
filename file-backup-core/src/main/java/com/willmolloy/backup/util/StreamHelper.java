package com.willmolloy.backup.util;

import static com.willmolloy.backup.util.Preconditions.require;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Helper methods for {@link Stream}.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public final class StreamHelper {

  /** Splits the given {@code stream} into chunks of size at most {@code size}. */
  public static <T> Stream<List<T>> chunk(Stream<T> stream, int size) {
    require(size >= 0);
    Iterator<T> input = stream.iterator();
    Iterable<List<T>> result =
        () ->
            new Iterator<>() {
              @Override
              public boolean hasNext() {
                return input.hasNext();
              }

              @SuppressFBWarnings(
                  value = "IT_NO_SUCH_ELEMENT",
                  justification = "Returns empty list")
              @Override
              public List<T> next() {
                return IntStream.iterate(0, i -> i < size && input.hasNext(), i -> i + 1)
                    .mapToObj(i -> input.next())
                    .toList();
              }
            };
    return StreamSupport.stream(result.spliterator(), false);
  }

  private StreamHelper() {}
}
