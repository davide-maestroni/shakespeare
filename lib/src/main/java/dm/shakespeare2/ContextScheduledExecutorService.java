package dm.shakespeare2;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import dm.shakespeare.util.ConstantConditions;
import dm.shakespeare2.actor.Behavior.Context;

/**
 * Created by davide-maestroni on 01/11/2019.
 */
class ContextScheduledExecutorService extends ContextExecutorService
    implements ScheduledExecutorService {

  private final ScheduledExecutorService mExecutor;

  ContextScheduledExecutorService(@NotNull final ScheduledExecutorService executor,
      @NotNull final Context context) {
    super(executor, context);
    mExecutor = executor;
  }

  @NotNull
  public ScheduledFuture<?> schedule(@NotNull final Runnable command, final long timeout,
      @NotNull final TimeUnit unit) {
    return wrap(addFuture(mExecutor.schedule(wrap(command), timeout, unit)));
  }

  @NotNull
  public <V> ScheduledFuture<V> schedule(@NotNull final Callable<V> task, final long timeout,
      @NotNull final TimeUnit unit) {
    return wrap(addFuture(mExecutor.schedule(wrap(task), timeout, unit)));
  }

  @NotNull
  public ScheduledFuture<?> scheduleAtFixedRate(@NotNull final Runnable command,
      final long initialDelay, final long period, @NotNull final TimeUnit unit) {
    return wrap(
        addFuture(mExecutor.scheduleAtFixedRate(wrap(command), initialDelay, period, unit)));
  }

  @NotNull
  public ScheduledFuture<?> scheduleWithFixedDelay(@NotNull final Runnable command,
      final long initialDelay, final long delay, @NotNull final TimeUnit unit) {
    return wrap(
        addFuture(mExecutor.scheduleWithFixedDelay(wrap(command), initialDelay, delay, unit)));
  }

  @NotNull
  private <V> ScheduledFuture<V> wrap(@NotNull final ScheduledFuture<V> future) {
    return new ContextScheduledFuture<V>(future);
  }

  private static class ContextScheduledFuture<V> implements ScheduledFuture<V> {

    private final ScheduledFuture<V> mFuture;

    private ContextScheduledFuture(@NotNull final ScheduledFuture<V> future) {
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

    public int compareTo(@NotNull final Delayed delayed) {
      return mFuture.compareTo(delayed);
    }

    public long getDelay(@NotNull final TimeUnit unit) {
      return mFuture.getDelay(unit);
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
      if (!(o instanceof ContextScheduledFuture)) {
        return false;
      }
      final ContextScheduledFuture<?> that = (ContextScheduledFuture<?>) o;
      return mFuture.equals(that.mFuture);
    }
  }
}
