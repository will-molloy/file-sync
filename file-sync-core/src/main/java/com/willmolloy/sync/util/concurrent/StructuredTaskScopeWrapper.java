package com.willmolloy.sync.util.concurrent;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Semaphore;
import jdk.incubator.concurrent.StructuredTaskScope;

/**
 * {@link StructuredTaskScope} decorated to make the API cleaner for the use cases of this app.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public final class StructuredTaskScopeWrapper implements AutoCloseable {
  private final StructuredTaskScope.ShutdownOnFailure delegate;
  private final Duration timeout = Duration.ofHours(1);
  private final Semaphore semaphore;

  public StructuredTaskScopeWrapper(String name, int concurrencyLimit) {
    this.delegate =
        new StructuredTaskScope.ShutdownOnFailure(
            name, Thread.ofVirtual().name("%s-worker-".formatted(name), 1).factory());
    this.semaphore = new Semaphore(concurrencyLimit);
  }

  public StructuredTaskScopeWrapper(String name) {
    this(name, Integer.MAX_VALUE);
  }

  /** Delegates to {@link StructuredTaskScope#fork}. */
  public void fork(UncheckedRunnable runnable) {
    delegate.fork(
        () -> {
          semaphore.acquire();
          try {
            runnable.run();
          } finally {
            semaphore.release();
          }
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
