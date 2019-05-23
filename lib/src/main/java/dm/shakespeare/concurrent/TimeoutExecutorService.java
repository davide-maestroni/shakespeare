/*
 * Copyright 2019 Davide Maestroni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dm.shakespeare.concurrent;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import dm.shakespeare.util.ConstantConditions;

/**
 * Class wrapping an {@code ExecutorService} instance so to limit the execution time of each
 * submitted task.
 */
class TimeoutExecutorService extends AbstractExecutorService {

  private static final Object mutex = new Object();

  private static long count;
  private static ScheduledExecutorService timeoutService;

  private final ExecutorService executorService;
  private final AtomicBoolean isShutdown = new AtomicBoolean();
  private final boolean mayInterruptIfRunning;
  private final TimeUnit timeUnit;
  private final long timeout;

  /**
   * Creates a new executor service wrapping the specified instance.
   *
   * @param executorService       the executor service to wrap.
   * @param timeout               the execution timeout.
   * @param timeUnit              the execution timeout unit.
   * @param mayInterruptIfRunning whether to interrupt running tasks when the timeout elapses.
   */
  TimeoutExecutorService(@NotNull final ExecutorService executorService, final long timeout,
      @NotNull final TimeUnit timeUnit, final boolean mayInterruptIfRunning) {
    this.executorService = ConstantConditions.notNull("executorService", executorService);
    this.timeout = ConstantConditions.positive("timeout", timeout);
    this.timeUnit = ConstantConditions.notNull("timeUnit", timeUnit);
    this.mayInterruptIfRunning = mayInterruptIfRunning;
    startGlobalService();
  }

  private static void shutdownGlobalService() {
    synchronized (mutex) {
      if (--count == 0) {
        timeoutService.shutdown();
      }
    }
  }

  private static void startGlobalService() {
    synchronized (mutex) {
      if (count++ == 0) {
        timeoutService = Executors.newSingleThreadScheduledExecutor();
      }
    }
  }

  public void execute(@NotNull final Runnable command) {
    timeout(executorService.submit(command));
  }

  public void shutdown() {
    executorService.shutdown();
    if (!isShutdown.getAndSet(true)) {
      shutdownGlobalService();
    }
  }

  @NotNull
  public List<Runnable> shutdownNow() {
    final List<Runnable> commands = executorService.shutdownNow();
    if (!isShutdown.getAndSet(true)) {
      shutdownGlobalService();
    }
    return commands;
  }

  public boolean isShutdown() {
    return executorService.isShutdown();
  }

  public boolean isTerminated() {
    return executorService.isTerminated();
  }

  public boolean awaitTermination(final long timeout, @NotNull final TimeUnit unit) throws
      InterruptedException {
    return executorService.awaitTermination(timeout, unit);
  }

  @NotNull
  <F extends Future<?>> F timeout(@NotNull final F future) {
    timeoutService.schedule(new CancelRunnable(future, mayInterruptIfRunning), timeout, timeUnit);
    return future;
  }

  private static class CancelRunnable implements Runnable {

    private final Future<?> future;
    private final boolean mayInterruptIfRunning;

    private CancelRunnable(@NotNull final Future<?> future, final boolean mayInterruptIfRunning) {
      this.future = future;
      this.mayInterruptIfRunning = mayInterruptIfRunning;
    }

    public void run() {
      future.cancel(mayInterruptIfRunning);
    }
  }
}
