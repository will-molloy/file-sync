package com.willmolloy.sync.util.concurrent;

import java.time.Duration;
import java.time.Instant;
import jdk.incubator.concurrent.StructuredTaskScope;

/**
 * {@link StructuredTaskScope} decorated to make the API cleaner for the use cases of this app.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public final class StructuredTaskScopeWrapper implements AutoCloseable {
  private final StructuredTaskScope.ShutdownOnFailure delegate;
  private final Duration timeout = Duration.ofHours(1);

  public StructuredTaskScopeWrapper(String name) {
    this.delegate =
        new StructuredTaskScope.ShutdownOnFailure(
            name, Thread.ofVirtual().name("%s-worker-".formatted(name), 0).factory());
  }

  /** Delegates to {@link StructuredTaskScope#fork}. */
  public void fork(UncheckedRunnable runnable) {
    delegate.fork(
        () -> {
          runnable.run();
          return null;
        });
  }

  @Override
  public void close() throws Exception {
    delegate.joinUntil(Instant.now().plus(timeout));
    delegate.throwIfFailed();
    delegate.close();
  }

  /** {@link Runnable} that can throw. */
  @FunctionalInterface
  public interface UncheckedRunnable {
    void run() throws Exception;
  }
}
