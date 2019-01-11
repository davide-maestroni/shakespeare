package dm.shakespeare2;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 01/11/2019.
 */
class ContextExecutorService implements ExecutorService {

  private final ExecutorService mExecutor;
  private final WeakHashMap<Future<?>, Void> mFutures = new WeakHashMap<Future<?>, Void>();

  ContextExecutorService(@NotNull final ExecutorService executor) {
    mExecutor = ConstantConditions.notNull("executor", executor);
  }

  public void execute(@NotNull final Runnable runnable) {
    mFutures.put(mExecutor.submit(runnable), null);
  }

  public void shutdown() {
    mExecutor.shutdown();
    mFutures.clear();
  }

  @NotNull
  public List<Runnable> shutdownNow() {
    final List<Runnable> runnables = mExecutor.shutdownNow();
    mFutures.clear();
    return runnables;
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
    return addFuture(mExecutor.submit(task));
  }

  @NotNull
  public <T> Future<T> submit(@NotNull final Runnable task, final T result) {
    return addFuture(mExecutor.submit(task, result));
  }

  @NotNull
  public Future<?> submit(@NotNull final Runnable task) {
    return addFuture(mExecutor.submit(task));
  }

  @NotNull
  public <T> List<Future<T>> invokeAll(
      @NotNull final Collection<? extends Callable<T>> tasks) throws InterruptedException {
    final List<Future<T>> futures = mExecutor.invokeAll(tasks);
    for (final Future<T> future : futures) {
      mFutures.put(future, null);
    }

    return futures;
  }

  @NotNull
  public <T> List<Future<T>> invokeAll(@NotNull final Collection<? extends Callable<T>> tasks,
      final long timeout, @NotNull final TimeUnit unit) throws InterruptedException {
    final List<Future<T>> futures = mExecutor.invokeAll(tasks, timeout, unit);
    for (final Future<T> future : futures) {
      mFutures.put(future, null);
    }

    return futures;
  }

  @NotNull
  public <T> T invokeAny(@NotNull final Collection<? extends Callable<T>> tasks) throws
      InterruptedException, ExecutionException {
    return mExecutor.invokeAny(tasks);
  }

  public <T> T invokeAny(@NotNull final Collection<? extends Callable<T>> tasks, final long timeout,
      @NotNull final TimeUnit unit) throws InterruptedException, ExecutionException,
      TimeoutException {
    return mExecutor.invokeAny(tasks, timeout, unit);
  }

  <V, F extends Future<V>> F addFuture(final F future) {
    if (future != null) {
      mFutures.put(future, null);
    }

    return future;
  }

  void cancelAll(final boolean mayInterruptIfRunning) {
    for (final Future<?> future : mFutures.keySet()) {
      future.cancel(mayInterruptIfRunning);
    }

    mFutures.clear();
  }
}
