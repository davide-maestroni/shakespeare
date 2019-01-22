package dm.shakespeare.executor;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import dm.shakespeare.util.ConstantConditions;
import dm.shakespeare.util.TimeUnits;
import dm.shakespeare.util.TimeUnits.Condition;

/**
 * Created by davide-maestroni on 09/24/2018.
 */
class CallableFuture<V> implements ScheduledFuture<V>, Runnable {

  private final Callable<V> mCallable;
  private final ExecutorService mExecutor;
  private final Object mMutex = new Object();
  private final long mTimestamp;

  private Future<V> mFuture;
  private ScheduledFuture<?> mScheduledFuture;

  CallableFuture(@NotNull final ExecutorService executor, @NotNull final Callable<V> callable,
      final long timestamp) {
    mExecutor = ConstantConditions.notNull("executor", executor);
    mCallable = ConstantConditions.notNull("callable", callable);
    mTimestamp = ConstantConditions.notNegative("timestamp", timestamp);
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
    }
    return false;
  }

  public V get() throws InterruptedException, ExecutionException {
    synchronized (mMutex) {
      if (TimeUnits.waitUntil(mMutex, new Condition() {

        public boolean isTrue() {
          return (mFuture != null);
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
          return (mFuture != null);
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
    return timeUnit.convert(mTimestamp - System.nanoTime(), TimeUnit.NANOSECONDS);
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

    if ((o == null) || (getClass() != o.getClass())) {
      return false;
    }
    final CallableFuture<?> that = (CallableFuture<?>) o;
    return (mTimestamp == that.mTimestamp) && (mFuture != null ? mFuture.equals(that.mFuture)
        : that.mFuture == null) && (mScheduledFuture != null ? mScheduledFuture.equals(
        that.mScheduledFuture) : that.mScheduledFuture == null);
  }

  public void run() {
    mFuture = mExecutor.submit(mCallable);
  }

  void setFuture(@NotNull final ScheduledFuture<?> future) {
    mScheduledFuture = future;
  }
}
