package com.willmolloy.backup.util;

/**
 * Preconditions for methods, constructors, etc.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public final class Preconditions {

  /**
   * Checks the given {@code predicate}; if {@code false} throws {@link IllegalArgumentException}.
   */
  public static void require(boolean predicate) {
    if (!predicate) {
      throw new IllegalArgumentException();
    }
  }

  /**
   * Checks the given {@code predicate}; if {@code false} throws {@link IllegalArgumentException}
   * with {@code errorMsg}.
   */
  public static void require(boolean predicate, String errorMsg) {
    if (!predicate) {
      throw new IllegalArgumentException(errorMsg);
    }
  }

  private Preconditions() {}
}
