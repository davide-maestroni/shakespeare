package dm.shakespeare.test.concurrent;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CancellationException;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide-maestroni on 02/14/2019.
 */
abstract class AbstractFuture<V> implements ScheduledFuture<V>, Runnable {

  private ExecutionException mException;
  private boolean mIsCancelled;
  private boolean mIsDone;
  private long mTimestamp;
  private V mValue;

  AbstractFuture(final long timestamp) {
    mTimestamp = timestamp;
  }

  static long toTimestampNanos(final long delay, @NotNull final TimeUnit unit) {
    return TimeUnit.NANOSECONDS.convert(Math.max(0, delay), unit);
  }

  public boolean cancel(final boolean mayInterruptIfRunning) {
    if (!mIsDone && !mIsCancelled) {
      mIsCancelled = true;
      return true;
    }
    return false;
  }

  public boolean isCancelled() {
    return mIsCancelled;
  }

  public boolean isDone() {
    return mIsDone;
  }

  public V get() throws InterruptedException, ExecutionException {
    if (mIsCancelled) {
      throw new CancellationException();
    }
    final ExecutionException exception = mException;
    if (exception != null) {
      throw exception;
    }

    if (!mIsDone) {
      throw new InterruptedException();
    }
    return mValue;
  }

  public V get(final long timeout, @NotNull final TimeUnit timeUnit) throws InterruptedException,
      ExecutionException {
    if (mIsCancelled) {
      throw new CancellationException();
    }
    final ExecutionException exception = mException;
    if (exception != null) {
      throw exception;
    }

    if (!mIsDone) {
      throw new InterruptedException();
    }
    return mValue;
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
    result = 31 * result + (mException != null ? mException.hashCode() : 0);
    result = 31 * result + (mIsCancelled ? 1 : 0);
    result = 31 * result + (mIsDone ? 1 : 0);
    result = 31 * result + (mValue != null ? mValue.hashCode() : 0);
    return result;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof AbstractFuture)) {
      return false;
    }
    final AbstractFuture<?> that = (AbstractFuture<?>) o;
    return (mTimestamp == that.mTimestamp) && (mIsCancelled == that.mIsCancelled) && (mIsDone
        == that.mIsDone) && (mException != null ? mException.equals(that.mException)
        : that.mException == null) && (mValue != null ? mValue.equals(that.mValue)
        : that.mValue == null);
  }

  public void run() {
    try {
      mValue = getValue();

    } catch (final Throwable t) {
      mException = new ExecutionException(t);
      if (t instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }

    } finally {
      mIsDone = true;
    }
  }

  long getTimestamp() {
    return mTimestamp;
  }

  void setTimestamp(final long timestamp) {
    mTimestamp = timestamp;
  }

  abstract V getValue() throws Exception;
}
