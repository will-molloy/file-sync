package com.willmolloy.backup.util;

import java.time.Duration;

/**
 * Helper methods for {@link java.time}.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public final class TimeHelper {

  public static Duration elapsed(long startNanos) {
    return Duration.ofNanos(System.nanoTime() - startNanos);
  }

  private TimeHelper() {}
}
