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

package dm.shakespeare;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import dm.shakespeare.actor.Behavior.Context;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 01/11/2019.
 */
class ContextExecutorService implements ExecutorService {

  private final Context mContext;
  private final ExecutorService mExecutor;
  private final WeakHashMap<Future<?>, Void> mTasks = new WeakHashMap<Future<?>, Void>();

  ContextExecutorService(@NotNull final ExecutorService executorService,
      @NotNull final Context context) {
    mExecutor = ConstantConditions.notNull("executorService", executorService);
    mContext = ConstantConditions.notNull("context", context);
  }

  public void execute(@NotNull final Runnable command) {
    mTasks.put(mExecutor.submit(command), null);
  }

  public void shutdown() {
    mExecutor.shutdown();
    mTasks.clear();
  }

  @NotNull
  public List<Runnable> shutdownNow() {
    final List<Runnable> commands = mExecutor.shutdownNow();
    mTasks.clear();
    return commands;
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
    return wrap(addFuture(mExecutor.submit(wrap(task))));
  }

  @NotNull
  public <T> Future<T> submit(@NotNull final Runnable task, final T result) {
    return wrap(addFuture(mExecutor.submit(wrap(task), result)));
  }

  @NotNull
  public Future<?> submit(@NotNull final Runnable task) {
    return wrap(addFuture(mExecutor.submit(wrap(task))));
  }

  @NotNull
  public <T> List<Future<T>> invokeAll(@NotNull final Collection<? extends Callable<T>> tasks) {
    return ConstantConditions.unsupported();
  }

  @NotNull
  public <T> List<Future<T>> invokeAll(@NotNull final Collection<? extends Callable<T>> tasks,
      final long timeout, @NotNull final TimeUnit unit) {
    return ConstantConditions.unsupported();
  }

  @NotNull
  public <T> T invokeAny(@NotNull final Collection<? extends Callable<T>> tasks) {
    return ConstantConditions.unsupported();
  }

  public <T> T invokeAny(@NotNull final Collection<? extends Callable<T>> tasks, final long timeout,
      @NotNull final TimeUnit unit) {
    return ConstantConditions.unsupported();
  }

  <V, F extends Future<V>> F addFuture(final F future) {
    if (future != null) {
      mTasks.put(future, null);
    }
    return future;
  }

  void cancelAll(final boolean mayInterruptIfRunning) {
    for (final Future<?> future : mTasks.keySet()) {
      future.cancel(mayInterruptIfRunning);
    }
    mTasks.clear();
  }

  @NotNull
  <V> Callable<V> wrap(@NotNull final Callable<V> task) {
    return new ContextCallable<V>(task, mContext);
  }

  @NotNull
  Runnable wrap(@NotNull final Runnable task) {
    return new ContextRunnable(task, mContext);
  }

  @NotNull
  private <V> Future<V> wrap(@NotNull final Future<V> future) {
    return new ContextFuture<V>(future);
  }

  private static class ContextCallable<V> implements Callable<V> {

    private final Context mContext;

    private final Callable<V> mTask;

    private ContextCallable(@NotNull final Callable<V> task, @NotNull final Context context) {
      mTask = ConstantConditions.notNull("task", task);
      mContext = context;
    }

    public V call() throws Exception {
      if (mContext.isDismissed()) {
        throw new IllegalStateException();
      }
      return mTask.call();
    }
  }

  private static class ContextFuture<V> implements Future<V> {

    private final Future<V> mFuture;

    private ContextFuture(@NotNull final Future<V> future) {
      mFuture = ConstantConditions.notNull("future", future);
    }

    public boolean cancel(final boolean mayInterruptIfRunning) {
      return mFuture.cancel(mayInterruptIfRunning);
    }

    public boolean isCancelled() {
      return mFuture.isCancelled();
    }

    public boolean isDone() {
      return mFuture.isDone();
    }

    public V get() {
      return ConstantConditions.unsupported();
    }

    public V get(final long timeout, @NotNull final TimeUnit unit) {
      return ConstantConditions.unsupported();
    }

    @Override
    public int hashCode() {
      return mFuture.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }

      if (!(o instanceof ContextFuture)) {
        return false;
      }
      final ContextFuture<?> that = (ContextFuture<?>) o;
      return mFuture.equals(that.mFuture);
    }
  }

  private static class ContextRunnable implements Runnable {

    private final Context mContext;

    private final Runnable mTask;

    private ContextRunnable(@NotNull final Runnable task, @NotNull final Context context) {
      mTask = ConstantConditions.notNull("task", task);
      mContext = context;
    }

    public void run() {
      if (!mContext.isDismissed()) {
        mTask.run();
      }
    }
  }
}
