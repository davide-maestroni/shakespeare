/*
 * Copyright 2018 Davide Maestroni
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

package dm.shakespeare2.executor;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import dm.shakespeare2.util.ConstantConditions;
import dm.shakespeare2.util.TimeUnits;
import dm.shakespeare2.util.TimeUnits.Condition;

/**
 * Scheduled thread pool executor wrapping an executor service.
 * <p>
 * Created by davide-maestroni on 05/24/2016.
 */
class ScheduledThreadPoolExecutorService extends ScheduledThreadPoolExecutor {

  private final ExecutorService mExecutor;

  /**
   * Constructor.
   *
   * @param service the executor service.
   */
  ScheduledThreadPoolExecutorService(@NotNull final ExecutorService service) {
    super(1);
    mExecutor = ConstantConditions.notNull("service", service);
  }

  @Override
  public boolean isShutdown() {
    return super.isShutdown() && mExecutor.isShutdown();
  }

  @Override
  public boolean isTerminating() {
    return super.isTerminating();
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
    return super.schedule(new ScheduledRunnable(mExecutor, command), delay, unit);
  }

  @NotNull
  @Override
  public <V> ScheduledFuture<V> schedule(final Callable<V> callable, final long delay,
      final TimeUnit unit) {
    final ExecutorFuture<V> future =
        new ExecutorFuture<V>(mExecutor, callable, TimeUnits.toTimestampNanos(delay, unit));
    future.setFuture(super.schedule(future, delay, unit));
    return future;
  }

  @NotNull
  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(final Runnable command, final long initialDelay,
      final long period, final TimeUnit unit) {
    return super.scheduleAtFixedRate(new ScheduledRunnable(mExecutor, command), initialDelay,
        period, unit);
  }

  @NotNull
  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(final Runnable command, final long initialDelay,
      final long delay, final TimeUnit unit) {
    return super.scheduleWithFixedDelay(new ScheduledRunnable(mExecutor, command), initialDelay,
        delay, unit);
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

  private static class ExecutorFuture<V> implements ScheduledFuture<V>, Runnable {

    private final Callable<V> mCallable;
    private final ExecutorService mExecutor;
    private final Object mMutex = new Object();
    private final long mTimestamp;

    private Future<V> mFuture;
    private ScheduledFuture<?> mScheduledFuture;

    private ExecutorFuture(@NotNull final ExecutorService executor,
        @NotNull final Callable<V> callable, final long timestamp) {
      mExecutor = executor;
      mCallable = callable;
      mTimestamp = timestamp;
    }

    public boolean cancel(final boolean mayInterruptIfRunning) {
      synchronized (mMutex) {
        final Future<V> future = mFuture;
        if (future != null) {
          return future.cancel(mayInterruptIfRunning);
        }
      }

      final ScheduledFuture<?> scheduledFuture = mScheduledFuture;
      return (scheduledFuture != null) && scheduledFuture.cancel(mayInterruptIfRunning);
    }

    public boolean isCancelled() {
      synchronized (mMutex) {
        final Future<V> future = mFuture;
        if (future != null) {
          return future.isCancelled();
        }
      }

      final ScheduledFuture<?> scheduledFuture = mScheduledFuture;
      return (scheduledFuture != null) && scheduledFuture.isCancelled();
    }

    public boolean isDone() {
      synchronized (mMutex) {
        final Future<V> future = mFuture;
        if (future != null) {
          return future.isDone();
        }

        final ScheduledFuture<?> scheduledFuture = mScheduledFuture;
        return (scheduledFuture != null) && scheduledFuture.isDone();
      }
    }

    public V get() throws InterruptedException, ExecutionException {
      synchronized (mMutex) {
        if (TimeUnits.waitUntil(mMutex, new Condition() {

          public boolean isTrue() {
            return (mScheduledFuture != null);
          }
        }, -1, TimeUnit.MILLISECONDS)) {
          return mFuture.get();
        }
      }

      throw new IllegalStateException();
    }

    public V get(final long timeout, @NotNull final TimeUnit timeUnit) throws InterruptedException,
        ExecutionException, TimeoutException {
      synchronized (mMutex) {
        final long startTime = System.currentTimeMillis();
        if (TimeUnits.waitUntil(mMutex, new Condition() {

          public boolean isTrue() {
            return (mScheduledFuture != null);
          }
        }, timeout, timeUnit)) {
          return mFuture.get(timeUnit.toMillis(timeout) + startTime - System.currentTimeMillis(),
              TimeUnit.MILLISECONDS);
        }
      }

      throw new TimeoutException();
    }

    public int compareTo(@NotNull final Delayed delayed) {
      if (delayed == this) {
        return 0;
      }

      return Long.valueOf(getDelay(TimeUnit.NANOSECONDS))
          .compareTo(delayed.getDelay(TimeUnit.NANOSECONDS));
    }

    public long getDelay(@NotNull final TimeUnit timeUnit) {
      return timeUnit.convert(mTimestamp, TimeUnit.NANOSECONDS);
    }

    @Override
    public int hashCode() {
      int result = (int) (mTimestamp ^ (mTimestamp >>> 32));
      result = 31 * result + (mFuture != null ? mFuture.hashCode() : 0);
      result = 31 * result + (mScheduledFuture != null ? mScheduledFuture.hashCode() : 0);
      return result;
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }

      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      final ExecutorFuture<?> that = (ExecutorFuture<?>) o;
      return (mTimestamp == that.mTimestamp) && (mFuture != null ? mFuture.equals(that.mFuture)
          : that.mFuture == null) && (mScheduledFuture != null ? mScheduledFuture.equals(
          that.mScheduledFuture) : that.mScheduledFuture == null);
    }

    public void run() {
      mFuture = mExecutor.submit(mCallable);
    }

    private void setFuture(@NotNull final ScheduledFuture<?> future) {
      synchronized (mMutex) {
        mScheduledFuture = future;
      }
    }
  }

  /**
   * Runnable executing another runnable.
   */
  private static class ScheduledRunnable implements Runnable {

    private final ExecutorService mExecutor;
    private final Runnable mRunnable;

    /**
     * Constructor.
     *
     * @param executor the executor service.
     * @param runnable the runnable to execute.
     */
    private ScheduledRunnable(@NotNull final ExecutorService executor,
        @NotNull final Runnable runnable) {
      mExecutor = executor;
      mRunnable = runnable;
    }

    public void run() {
      mExecutor.execute(mRunnable);
    }
  }
}
