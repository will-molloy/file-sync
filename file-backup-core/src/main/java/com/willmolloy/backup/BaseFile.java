package com.willmolloy.backup;

import java.util.Objects;

/**
 * Base {@link File} class with common methods implemented for convenience.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public abstract class BaseFile implements File {

  @Override
  public final boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null) {
      return false;
    }
    if (o instanceof File file) {
      return Objects.equals(uri(), file.uri());
    }
    return false;
  }

  @Override
  public final int hashCode() {
    return Objects.hash(uri());
  }

  @Override
  public final String toString() {
    return "%s[%s]".formatted(getClass().getSimpleName(), uri());
  }
}
