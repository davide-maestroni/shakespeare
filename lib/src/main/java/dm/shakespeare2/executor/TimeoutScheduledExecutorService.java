package dm.shakespeare2.executor;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide-maestroni on 06/06/2018.
 */
class TimeoutScheduledExecutorService extends TimeoutExecutorService
    implements ScheduledExecutorService {

  private final ScheduledExecutorService mExecutor;

  TimeoutScheduledExecutorService(@NotNull final ScheduledExecutorService executor,
      final long timeout, @NotNull final TimeUnit timeUnit, final boolean mayInterruptIfRunning) {
    super(executor, timeout, timeUnit, mayInterruptIfRunning);
    mExecutor = executor;
  }

  @NotNull
  public ScheduledFuture<?> schedule(@NotNull final Runnable runnable, final long delay,
      @NotNull final TimeUnit unit) {
    return timeout(mExecutor.schedule(runnable, delay, unit));
  }

  @NotNull
  public <V> ScheduledFuture<V> schedule(@NotNull final Callable<V> callable, final long delay,
      @NotNull final TimeUnit unit) {
    return timeout(mExecutor.schedule(callable, delay, unit));
  }

  @NotNull
  public ScheduledFuture<?> scheduleAtFixedRate(@NotNull final Runnable runnable,
      final long initialDelay, final long period, @NotNull final TimeUnit unit) {
    return timeout(mExecutor.scheduleAtFixedRate(runnable, initialDelay, period, unit));
  }

  @NotNull
  public ScheduledFuture<?> scheduleWithFixedDelay(@NotNull final Runnable runnable,
      final long initialDelay, final long delay, @NotNull final TimeUnit unit) {
    return timeout(mExecutor.scheduleWithFixedDelay(runnable, initialDelay, delay, unit));
  }
}
