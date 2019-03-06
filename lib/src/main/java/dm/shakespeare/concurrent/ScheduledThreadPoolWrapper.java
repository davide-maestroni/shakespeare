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
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import dm.shakespeare.util.ConstantConditions;
import dm.shakespeare.util.TimeUnits;

/**
 * Scheduled thread pool executor wrapping an executor service.
 * <p>
 * Created by davide-maestroni on 05/24/2016.
 */
class ScheduledThreadPoolWrapper extends ScheduledThreadPoolExecutor {

  private final ExecutorService mExecutor;

  /**
   * Constructor.
   *
   * @param executor the executor service.
   */
  ScheduledThreadPoolWrapper(@NotNull final ExecutorService executor) {
    super(1);
    if (executor instanceof QueuedExecutorService) {
      mExecutor = new NextExecutorService((QueuedExecutorService) executor);

    } else {
      mExecutor = ConstantConditions.notNull("executor", executor);
    }
  }

  /**
   * Constructor.
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
    this(new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, keepAliveUnit,
        new LinkedBlockingQueue<Runnable>()));
  }

  /**
   * Constructor.
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
    this(new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, keepAliveUnit,
        new LinkedBlockingQueue<Runnable>(), threadFactory));
  }

  @Override
  public boolean isShutdown() {
    return super.isShutdown() && mExecutor.isShutdown();
  }

  @Override
  public boolean isTerminated() {
    return super.isTerminated() && mExecutor.isTerminated();
  }

  @Override
  public boolean awaitTermination(final long timeout, @NotNull final TimeUnit unit) throws
      InterruptedException {
    final long startTime = System.currentTimeMillis();
    if (super.awaitTermination(timeout, unit)) {
      long remainingTime = System.currentTimeMillis() - startTime - unit.toMillis(timeout);
      return (remainingTime > 0) && mExecutor.awaitTermination(remainingTime,
          TimeUnit.MILLISECONDS);
    }
    return false;
  }

  @NotNull
  @Override
  public ScheduledFuture<?> schedule(final Runnable command, final long delay,
      final TimeUnit unit) {
    final RunnableFuture future =
        new RunnableFuture(mExecutor, command, TimeUnits.toTimestampNanos(delay, unit));
    future.setFuture(super.schedule(future, delay, unit));
    return future;
  }

  @NotNull
  @Override
  public <V> ScheduledFuture<V> schedule(final Callable<V> callable, final long delay,
      final TimeUnit unit) {
    final CallableFuture<V> future =
        new CallableFuture<V>(mExecutor, callable, TimeUnits.toTimestampNanos(delay, unit));
    future.setFuture(super.schedule(future, delay, unit));
    return future;
  }

  @NotNull
  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(final Runnable command, final long initialDelay,
      final long period, final TimeUnit unit) {
    final RunnableFuture future =
        new RunnableFuture(mExecutor, command, TimeUnits.toTimestampNanos(initialDelay, unit),
            unit.toNanos(ConstantConditions.positive("period", period)));
    future.setFuture(super.scheduleAtFixedRate(future, initialDelay, period, unit));
    return future;
  }

  @NotNull
  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(final Runnable command, final long initialDelay,
      final long delay, final TimeUnit unit) {
    final RunnableFuture future =
        new RunnableFuture(mExecutor, command, TimeUnits.toTimestampNanos(initialDelay, unit),
            unit.toNanos(-ConstantConditions.positive("delay", delay)));
    future.setFuture(super.scheduleWithFixedDelay(future, initialDelay, delay, unit));
    return future;
  }

  @Override
  public void execute(final Runnable command) {
    mExecutor.execute(command);
  }

  @NotNull
  @Override
  public Future<?> submit(final Runnable task) {
    return mExecutor.submit(task);
  }

  @NotNull
  @Override
  public <T> Future<T> submit(final Runnable task, final T result) {
    return mExecutor.submit(task, result);
  }

  @NotNull
  @Override
  public <T> Future<T> submit(final Callable<T> task) {
    return mExecutor.submit(task);
  }

  @Override
  public void shutdown() {
    super.shutdown();
    mExecutor.shutdown();
  }

  @NotNull
  @Override
  public List<Runnable> shutdownNow() {
    final ArrayList<Runnable> runnables = new ArrayList<Runnable>(super.shutdownNow());
    runnables.addAll(mExecutor.shutdownNow());
    return runnables;
  }

  private static class NextExecutorService implements ExecutorService {

    private final QueuedExecutorService mExecutor;

    private NextExecutorService(@NotNull final QueuedExecutorService executor) {
      mExecutor = executor;
    }

    public void execute(@NotNull final Runnable command) {
      mExecutor.executeNext(command);
    }

    public void shutdown() {
      mExecutor.shutdown();
    }

    @NotNull
    public List<Runnable> shutdownNow() {
      return mExecutor.shutdownNow();
    }

    public boolean isShutdown() {
      return mExecutor.isShutdown();
    }

    public boolean isTerminated() {
      return mExecutor.isTerminated();
    }

    public boolean awaitTermination(final long timeout, @NotNull final TimeUnit unit) throws
        InterruptedException {
      return mExecutor.awaitTermination(timeout, unit);
    }

    @NotNull
    public <T> Future<T> submit(@NotNull final Callable<T> task) {
      return mExecutor.submit(task);
    }

    @NotNull
    public <T> Future<T> submit(@NotNull final Runnable task, final T result) {
      return mExecutor.submit(task, result);
    }

    @NotNull
    public Future<?> submit(@NotNull final Runnable task) {
      return mExecutor.submit(task);
    }

    @NotNull
    public <T> List<Future<T>> invokeAll(
        @NotNull final Collection<? extends Callable<T>> tasks) throws InterruptedException {
      return mExecutor.invokeAll(tasks);
    }

    @NotNull
    public <T> List<Future<T>> invokeAll(@NotNull final Collection<? extends Callable<T>> tasks,
        final long timeout, @NotNull final TimeUnit unit) throws InterruptedException {
      return mExecutor.invokeAll(tasks, timeout, unit);
    }

    @NotNull
    public <T> T invokeAny(@NotNull final Collection<? extends Callable<T>> tasks) throws
        InterruptedException, ExecutionException {
      return mExecutor.invokeAny(tasks);
    }

    public <T> T invokeAny(@NotNull final Collection<? extends Callable<T>> tasks,
        final long timeout, @NotNull final TimeUnit unit) throws InterruptedException,
        ExecutionException, TimeoutException {
      return mExecutor.invokeAny(tasks, timeout, unit);
    }
  }
}

