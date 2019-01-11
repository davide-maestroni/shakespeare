package dm.shakespeare.executor;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import dm.shakespeare.util.ConstantConditions;
import dm.shakespeare.util.TimeUnits;
import dm.shakespeare.util.TimeUnits.Condition;

/**
 * Created by davide-maestroni on 09/24/2018.
 */
class RunnableFuture implements ScheduledFuture<Object>, Runnable {

  private final ExecutorService mExecutor;
  private final Object mMutex = new Object();
  private final long mPeriod;
  private final Runnable mRunnable;

  private Future<?> mFuture;
  private ScheduledFuture<?> mScheduledFuture;
  private AtomicLong mTimestamp;

  RunnableFuture(@NotNull final ExecutorService executor, @NotNull final Runnable runnable,
      final long timestamp) {
    this(executor, runnable, timestamp, 0);
  }

  RunnableFuture(@NotNull final ExecutorService executor, @NotNull final Runnable runnable,
      final long timestamp, final long period) {
    mExecutor = ConstantConditions.notNull("executor", executor);
    mRunnable = ConstantConditions.notNull("runnable", runnable);
    mTimestamp = new AtomicLong(timestamp);
    mPeriod = period;
  }

  public boolean cancel(final boolean mayInterruptIfRunning) {
    final ScheduledFuture<?> scheduledFuture = mScheduledFuture;
    boolean isCancelled =
        (scheduledFuture != null) && scheduledFuture.cancel(mayInterruptIfRunning);
    synchronized (mMutex) {
      final Future<?> future = mFuture;
      if (future != null) {
        isCancelled |= future.cancel(mayInterruptIfRunning);
      }
    }

    return isCancelled;
  }

  public boolean isCancelled() {
    final ScheduledFuture<?> scheduledFuture = mScheduledFuture;
    boolean isCancelled = (scheduledFuture != null) && scheduledFuture.isCancelled();
    synchronized (mMutex) {
      final Future<?> future = mFuture;
      if (future != null) {
        isCancelled |= future.isCancelled();
      }
    }

    return isCancelled;
  }

  public boolean isDone() {
    final ScheduledFuture<?> scheduledFuture = mScheduledFuture;
    boolean isDone = (scheduledFuture != null) && scheduledFuture.isDone();
    if (isDone) {
      synchronized (mMutex) {
        final Future<?> future = mFuture;
        if (future != null) {
          return future.isDone();
        }
      }
    }

    return isDone;
  }

  public Object get() throws InterruptedException, ExecutionException {
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

  public Object get(final long timeout, @NotNull final TimeUnit timeUnit) throws
      InterruptedException, ExecutionException, TimeoutException {
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
    return timeUnit.convert(mTimestamp.get() - System.nanoTime(), TimeUnit.NANOSECONDS);
  }

  @Override
  public int hashCode() {
    int result = (int) (mPeriod ^ (mPeriod >>> 32));
    result = 31 * result + (mFuture != null ? mFuture.hashCode() : 0);
    result = 31 * result + (mScheduledFuture != null ? mScheduledFuture.hashCode() : 0);
    result = 31 * result + (mTimestamp != null ? mTimestamp.hashCode() : 0);
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

    final RunnableFuture that = (RunnableFuture) o;
    return (mPeriod == that.mPeriod) && (mFuture != null ? mFuture.equals(that.mFuture)
        : that.mFuture == null) && (mScheduledFuture != null ? mScheduledFuture.equals(
        that.mScheduledFuture) : that.mScheduledFuture == null) && (mTimestamp != null
        ? mTimestamp.equals(that.mTimestamp) : that.mTimestamp == null);
  }

  void setFuture(@NotNull final ScheduledFuture<?> future) {
    mScheduledFuture = future;
  }

  private void updateTimestamp() {
    long period = mPeriod;
    if (period == 0) {
      return;
    }

    if (period > 0) {
      mTimestamp.addAndGet(period);

    } else {
      mTimestamp.set(TimeUnits.toTimestampNanos(-period, TimeUnit.NANOSECONDS));
    }
  }

  public void run() {
    mFuture = mExecutor.submit(new Runnable() {

      public void run() {
        mRunnable.run();
        updateTimestamp();
      }
    });
  }
}
