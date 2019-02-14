package dm.shakespeare.test.concurrent;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.TimeUnit;

import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 02/14/2019.
 */
class RunnableFuture extends AbstractFuture<Object> {

  private final List<AbstractFuture<?>> mFutures;
  private final long mPeriod;
  private final Runnable mRunnable;

  RunnableFuture(@NotNull final List<AbstractFuture<?>> futures, @NotNull final Runnable runnable,
      final long timestamp) {
    this(futures, runnable, timestamp, 0);
  }

  RunnableFuture(@NotNull final List<AbstractFuture<?>> futures, @NotNull final Runnable runnable,
      final long timestamp, final long period) {
    super(timestamp);
    mFutures = ConstantConditions.notNull("futures", futures);
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

  Object getValue() {
    mRunnable.run();
    if (updateTimestamp()) {
      mFutures.add(this);
    }
    return null;
  }

  private boolean updateTimestamp() {
    long period = mPeriod;
    if (period == 0) {
      return false;
    }

    if (period > 0) {
      setTimestamp(getTimestamp() + period);

    } else {
      setTimestamp(AbstractFuture.toTimestampNanos(-period, TimeUnit.NANOSECONDS));
    }
    return true;
  }
}
