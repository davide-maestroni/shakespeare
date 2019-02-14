package dm.shakespeare.concurrent;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import dm.shakespeare.util.ConstantConditions;
import dm.shakespeare.util.TimeUnits;

/**
 * Created by davide-maestroni on 09/24/2018.
 */
class RunnableFuture extends AbstractFuture<Object> {

  private final long mPeriod;
  private final Runnable mRunnable;

  RunnableFuture(@NotNull final ExecutorService executor, @NotNull final Runnable runnable,
      final long timestamp) {
    this(executor, runnable, timestamp, 0);
  }

  RunnableFuture(@NotNull final ExecutorService executor, @NotNull final Runnable runnable,
      final long timestamp, final long period) {
    super(executor, timestamp);
    mRunnable = ConstantConditions.notNull("runnable", runnable);
    mPeriod = period;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (int) (mPeriod ^ (mPeriod >>> 32));
    result = 31 * result + mRunnable.hashCode();
    return result;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }

    if ((o == null) || getClass() != o.getClass()) {
      return false;
    }

    if (!super.equals(o)) {
      return false;
    }
    final RunnableFuture that = (RunnableFuture) o;
    return (mPeriod == that.mPeriod) && mRunnable.equals(that.mRunnable);
  }

  @NotNull
  @SuppressWarnings("unchecked")
  Future<Object> submitTo(@NotNull final ExecutorService executor) {
    return (Future<Object>) executor.submit(new Runnable() {

      public void run() {
        mRunnable.run();
        updateTimestamp();
      }
    });
  }

  private void updateTimestamp() {
    long period = mPeriod;
    if (period == 0) {
      return;
    }

    if (period > 0) {
      getTimestamp().addAndGet(period);

    } else {
      getTimestamp().set(TimeUnits.toTimestampNanos(-period, TimeUnit.NANOSECONDS));
    }
  }
}
