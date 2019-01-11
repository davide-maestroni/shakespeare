package dm.shakespeare.executor;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import dm.shakespeare.util.ConstantConditions;
import dm.shakespeare.util.TimeUnits;

/**
 * Created by davide-maestroni on 06/06/2018.
 */
class ThrottledScheduledExecutorService extends ThrottledExecutorService
    implements ScheduledExecutorService {

  private final ScheduledExecutorService mExecutor;

  ThrottledScheduledExecutorService(@NotNull final ScheduledExecutorService executor,
      final int maxConcurrency) {
    super(executor, maxConcurrency);
    mExecutor = executor;
  }

  @NotNull
  public ScheduledFuture<?> schedule(@NotNull final Runnable command, final long delay,
      @NotNull final TimeUnit unit) {
    final RunnableFuture future =
        new RunnableFuture(this, command, TimeUnits.toTimestampNanos(delay, unit));
    future.setFuture(mExecutor.schedule(future, delay, unit));
    return future;
  }

  @NotNull
  public <V> ScheduledFuture<V> schedule(@NotNull final Callable<V> callable, final long delay,
      @NotNull final TimeUnit unit) {
    final CallableFuture<V> future =
        new CallableFuture<V>(this, callable, TimeUnits.toTimestampNanos(delay, unit));
    future.setFuture(mExecutor.schedule(future, delay, unit));
    return future;
  }

  @NotNull
  public ScheduledFuture<?> scheduleAtFixedRate(@NotNull final Runnable command,
      final long initialDelay, final long period, @NotNull final TimeUnit unit) {
    final RunnableFuture future =
        new RunnableFuture(this, command, TimeUnits.toTimestampNanos(initialDelay, unit),
            unit.toNanos(ConstantConditions.positive("period", period)));
    future.setFuture(mExecutor.scheduleAtFixedRate(future, initialDelay, period, unit));
    return future;
  }

  @NotNull
  public ScheduledFuture<?> scheduleWithFixedDelay(@NotNull final Runnable command,
      final long initialDelay, final long delay, @NotNull final TimeUnit unit) {
    final RunnableFuture future =
        new RunnableFuture(this, command, TimeUnits.toTimestampNanos(initialDelay, unit),
            unit.toNanos(-ConstantConditions.positive("delay", delay)));
    future.setFuture(mExecutor.scheduleWithFixedDelay(future, initialDelay, delay, unit));
    return future;
  }
}
