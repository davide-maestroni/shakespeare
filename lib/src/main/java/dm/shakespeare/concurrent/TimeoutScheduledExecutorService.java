package dm.shakespeare.concurrent;

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
  public ScheduledFuture<?> schedule(@NotNull final Runnable command, final long delay,
      @NotNull final TimeUnit unit) {
    return timeout(mExecutor.schedule(command, delay, unit));
  }

  @NotNull
  public <V> ScheduledFuture<V> schedule(@NotNull final Callable<V> callable, final long delay,
      @NotNull final TimeUnit unit) {
    return timeout(mExecutor.schedule(callable, delay, unit));
  }

  @NotNull
  public ScheduledFuture<?> scheduleAtFixedRate(@NotNull final Runnable command,
      final long initialDelay, final long period, @NotNull final TimeUnit unit) {
    return timeout(mExecutor.scheduleAtFixedRate(command, initialDelay, period, unit));
  }

  @NotNull
  public ScheduledFuture<?> scheduleWithFixedDelay(@NotNull final Runnable command,
      final long initialDelay, final long delay, @NotNull final TimeUnit unit) {
    return timeout(mExecutor.scheduleWithFixedDelay(command, initialDelay, delay, unit));
  }
}
