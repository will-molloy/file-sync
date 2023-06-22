package com.willmolloy.sync.util.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;

/**
 * Thread Pools.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
public final class ThreadPools {

  /** Virtual thread pool. */
  public static ExecutorService virtual(String name) {
    return Executors.newThreadPerTaskExecutor(
        Thread.ofVirtual().name("%s-worker-".formatted(name), 1).factory());
  }

  /** Fork-join thread pool. */
  public static ForkJoinPool forkJoin(String name) {
    ForkJoinPool.ForkJoinWorkerThreadFactory factory =
        pool -> {
          ForkJoinWorkerThread worker =
              ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
          worker.setName("%s-worker-%d".formatted(name, worker.getPoolIndex()));
          return worker;
        };
    return new ForkJoinPool(Runtime.getRuntime().availableProcessors(), factory, null, false);
  }

  private ThreadPools() {}
}
