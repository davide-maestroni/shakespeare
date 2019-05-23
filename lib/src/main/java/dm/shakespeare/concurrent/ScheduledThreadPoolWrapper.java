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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import dm.shakespeare.util.ConstantConditions;
import dm.shakespeare.util.TimeUnits;

/**
 * Scheduled thread pool executor wrapping an executor service.<br>
 * The service maintains a fixed number of always available threads, while adding new ones till
 * reaching a maximum number. All the threads exceeding the core ones will be automatically
 * stopped if they stay idle for the specified amount of time.
 */
class ScheduledThreadPoolWrapper extends ScheduledThreadPoolExecutor {

  private final ExecutorService executorService;

  /**
   * Creates a new executor service wrapping the specified instance.
   *
   * @param executorService the executor service to wrap.
   */
  ScheduledThreadPoolWrapper(@NotNull final ExecutorService executorService) {
    super(1);
    this.executorService = ConstantConditions.notNull("executorService", executorService);
  }

  /**
   * Creates a new executor service with the specified configuration.
   *
   * @param corePoolSize    the number of threads to keep in the pool, even if they are idle.
   * @param maximumPoolSize the maximum number of threads to allow in the pool.
   * @param keepAliveTime   when the number of threads is greater than the core, this is the
   *                        maximum time that excess idle threads will wait for new tasks before
   *                        terminating.
   * @param keepAliveUnit   the time unit for the keep alive time.
   * @throws IllegalArgumentException if one of the following holds:<ul>
   *                                  <li>{@code corePoolSize < 0}</li>
   *                                  <li>{@code maximumPoolSize <= 0}</li>
   *                                  <li>{@code keepAliveTime < 0}</li></ul>
   */
  ScheduledThreadPoolWrapper(final int corePoolSize, final int maximumPoolSize,
      final long keepAliveTime, @NotNull final TimeUnit keepAliveUnit) {
    super(1);
    executorService =
        new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, keepAliveUnit,
            new LinkedBlockingQueue<Runnable>());
  }

  /**
   * Creates a new executor service with the specified configuration.
   *
   * @param corePoolSize    the number of threads to keep in the pool, even if they are idle.
   * @param maximumPoolSize the maximum number of threads to allow in the pool.
   * @param keepAliveTime   when the number of threads is greater than the core, this is the
   *                        maximum time that excess idle threads will wait for new tasks before
   *                        terminating.
   * @param keepAliveUnit   the time unit for the keep alive time.
   * @param threadFactory   the thread factory.
   * @throws IllegalArgumentException if one of the following holds:<ul>
   *                                  <li>{@code corePoolSize < 0}</li>
   *                                  <li>{@code maximumPoolSize <= 0}</li>
   *                                  <li>{@code keepAliveTime < 0}</li></ul>
   */
  ScheduledThreadPoolWrapper(final int corePoolSize, final int maximumPoolSize,
      final long keepAliveTime, @NotNull final TimeUnit keepAliveUnit,
      @NotNull final ThreadFactory threadFactory) {
    super(1);
    executorService =
        new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, keepAliveUnit,
            new LinkedBlockingQueue<Runnable>(), threadFactory);
  }

  @Override
  public boolean isShutdown() {
    return super.isShutdown() && executorService.isShutdown();
  }

  @Override
  public boolean isTerminated() {
    return super.isTerminated() && executorService.isTerminated();
  }

  @Override
  public boolean awaitTermination(final long timeout, @NotNull final TimeUnit unit) throws
      InterruptedException {
    final long startTime = System.currentTimeMillis();
    if (super.awaitTermination(timeout, unit)) {
      long remainingTime = unit.toMillis(timeout) + startTime - System.currentTimeMillis();
      return (remainingTime > 0) && executorService.awaitTermination(remainingTime,
          TimeUnit.MILLISECONDS);
    }
    return false;
  }

  @NotNull
  @Override
  public ScheduledFuture<?> schedule(final Runnable command, final long delay,
      final TimeUnit unit) {
    final RunnableFuture future =
        new RunnableFuture(executorService, command, TimeUnits.toTimestampNanos(delay, unit));
    future.setFuture(super.schedule(future, delay, unit));
    return future;
  }

  @NotNull
  @Override
  public <V> ScheduledFuture<V> schedule(final Callable<V> callable, final long delay,
      final TimeUnit unit) {
    final CallableFuture<V> future =
        new CallableFuture<V>(executorService, callable, TimeUnits.toTimestampNanos(delay, unit));
    future.setFuture(super.schedule(future, delay, unit));
    return future;
  }

  @NotNull
  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(final Runnable command, final long initialDelay,
      final long period, final TimeUnit unit) {
    final RunnableFuture future =
        new RunnableFuture(executorService, command, TimeUnits.toTimestampNanos(initialDelay, unit),
            unit.toNanos(ConstantConditions.positive("period", period)));
    future.setFuture(super.scheduleAtFixedRate(future, initialDelay, period, unit));
    return future;
  }

  @NotNull
  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(final Runnable command, final long initialDelay,
      final long delay, final TimeUnit unit) {
    final RunnableFuture future =
        new RunnableFuture(executorService, command, TimeUnits.toTimestampNanos(initialDelay, unit),
            unit.toNanos(-ConstantConditions.positive("delay", delay)));
    future.setFuture(super.scheduleWithFixedDelay(future, initialDelay, delay, unit));
    return future;
  }

  @Override
  public void execute(final Runnable command) {
    executorService.execute(command);
  }

  @NotNull
  @Override
  public Future<?> submit(final Runnable task) {
    return executorService.submit(task);
  }

  @NotNull
  @Override
  public <T> Future<T> submit(final Runnable task, final T result) {
    return executorService.submit(task, result);
  }

  @NotNull
  @Override
  public <T> Future<T> submit(final Callable<T> task) {
    return executorService.submit(task);
  }

  @Override
  public void shutdown() {
    executorService.shutdown();
    super.shutdown();
  }

  @NotNull
  @Override
  public List<Runnable> shutdownNow() {
    final ArrayList<Runnable> commands = new ArrayList<Runnable>(executorService.shutdownNow());
    commands.addAll(super.shutdownNow());
    return commands;
  }
}

