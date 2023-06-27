package com.willmolloy.sync.util.concurrent;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import jdk.incubator.concurrent.StructuredTaskScope;

/**
 * {@link StructuredTaskScope} decorated to make the API cleaner for the use cases of this app
 * ({@code void} tasks, implicit {@code join}, etc.).
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public final class VoidStructuredTaskScope extends StructuredTaskScope<Void> {
  private final StructuredTaskScope.ShutdownOnFailure delegate;

  public VoidStructuredTaskScope(String name) {
    this.delegate =
        new StructuredTaskScope.ShutdownOnFailure(
            name, Thread.ofVirtual().name("%s-worker".formatted(name), 0).factory());
  }

  public void fork(Runnable task) {
    delegate.fork(Executors.callable(task));
  }

  @Override
  public void close() {
    try {
      delegate.join();
      delegate.throwIfFailed();
      delegate.close();
    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
