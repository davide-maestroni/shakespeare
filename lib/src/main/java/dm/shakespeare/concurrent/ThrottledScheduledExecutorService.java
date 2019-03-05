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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import dm.shakespeare.util.ConstantConditions;
import dm.shakespeare.util.TimeUnits;

/**
 * Created by davide-maestroni on 06/06/2018.
 */
class ThrottledScheduledExecutorService extends ThrottledExecutorService
    implements ScheduledExecutorService {

  private final ScheduledExecutorService mExecutor;
  private final NextExecutorService mNextExecutor;

  ThrottledScheduledExecutorService(@NotNull final ScheduledExecutorService executor,
      final int maxConcurrency) {
    super(executor, maxConcurrency);
    mExecutor = executor;
    mNextExecutor = new NextExecutorService(this);
  }

  @NotNull
  public ScheduledFuture<?> schedule(@NotNull final Runnable command, final long delay,
      @NotNull final TimeUnit unit) {
    final RunnableFuture future =
        new RunnableFuture(mNextExecutor, command, TimeUnits.toTimestampNanos(delay, unit));
    future.setFuture(mExecutor.schedule(future, delay, unit));
    return future;
  }

  @NotNull
  public <V> ScheduledFuture<V> schedule(@NotNull final Callable<V> callable, final long delay,
      @NotNull final TimeUnit unit) {
    final CallableFuture<V> future =
        new CallableFuture<V>(mNextExecutor, callable, TimeUnits.toTimestampNanos(delay, unit));
    future.setFuture(mExecutor.schedule(future, delay, unit));
    return future;
  }

  @NotNull
  public ScheduledFuture<?> scheduleAtFixedRate(@NotNull final Runnable command,
      final long initialDelay, final long period, @NotNull final TimeUnit unit) {
    final RunnableFuture future =
        new RunnableFuture(mNextExecutor, command, TimeUnits.toTimestampNanos(initialDelay, unit),
            unit.toNanos(ConstantConditions.positive("period", period)));
    future.setFuture(mExecutor.scheduleAtFixedRate(future, initialDelay, period, unit));
    return future;
  }

  @NotNull
  public ScheduledFuture<?> scheduleWithFixedDelay(@NotNull final Runnable command,
      final long initialDelay, final long delay, @NotNull final TimeUnit unit) {
    final RunnableFuture future =
        new RunnableFuture(mNextExecutor, command, TimeUnits.toTimestampNanos(initialDelay, unit),
            unit.toNanos(-ConstantConditions.positive("delay", delay)));
    future.setFuture(mExecutor.scheduleWithFixedDelay(future, initialDelay, delay, unit));
    return future;
  }

  private static class NextExecutorService implements ExecutorService {

    private final ThrottledExecutorService mExecutor;

    private NextExecutorService(@NotNull final ThrottledExecutorService executor) {
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
