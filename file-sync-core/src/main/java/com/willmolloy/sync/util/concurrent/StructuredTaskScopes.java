package com.willmolloy.sync.util.concurrent;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import jdk.incubator.concurrent.StructuredTaskScope;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Helpers for {@link StructuredTaskScope}.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public final class StructuredTaskScopes {
  private static final Logger log = LogManager.getLogger();

  private static final Duration TIMEOUT = Duration.ofHours(1);

  /** Runs {@code runnable} in a {@link StructuredTaskScope}. */
  public static void runInScope(String name, ScopedRunnable runnable) {
    try (StructuredTaskScope.ShutdownOnFailure scope =
        new StructuredTaskScope.ShutdownOnFailure(
            name, Thread.ofVirtual().name("%s-worker-".formatted(name), 1).factory())) {
      scope.fork(
          () -> {
            runnable.run(scope);
            return null;
          });
      scope.joinUntil(Instant.now().plus(TIMEOUT));
      scope.throwIfFailed();
    } catch (ExecutionException | InterruptedException | TimeoutException e) {
      log.error("Error running", e);
      throw new RuntimeException(e);
    }
  }

  /** See {@link #runInScope}. */
  @FunctionalInterface
  public interface ScopedRunnable {
    void run(StructuredTaskScope<Object> scope) throws Exception;
  }

  /** Adapts {@link UncheckedRunnable} to {@link Callable}. */
  public static Callable<?> callable(UncheckedRunnable runnable) {
    return () -> {
      runnable.run();
      return null;
    };
  }

  /** See {@link #callable}. */
  @FunctionalInterface
  public interface UncheckedRunnable {
    void run() throws Exception;
  }

  private StructuredTaskScopes() {}
}
