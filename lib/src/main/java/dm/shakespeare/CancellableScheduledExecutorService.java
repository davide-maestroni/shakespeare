package dm.shakespeare;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide-maestroni on 06/06/2018.
 */
class CancellableScheduledExecutorService extends CancellableExecutorService
    implements ScheduledExecutorService {

  private final ScheduledExecutorService mExecutor;

  CancellableScheduledExecutorService(@NotNull final ScheduledExecutorService executor) {
    super(executor);
    mExecutor = executor;
  }

  @NotNull
  public ScheduledFuture<?> schedule(@NotNull final Runnable runnable, final long delay,
      @NotNull final TimeUnit unit) {
    return mExecutor.schedule(runnable, delay, unit);
  }

  @NotNull
  public <V> ScheduledFuture<V> schedule(@NotNull final Callable<V> callable, final long delay,
      @NotNull final TimeUnit unit) {
    return mExecutor.schedule(callable, delay, unit);
  }

  @NotNull
  public ScheduledFuture<?> scheduleAtFixedRate(@NotNull final Runnable runnable,
      final long initialDelay, final long period, @NotNull final TimeUnit unit) {
    return mExecutor.scheduleAtFixedRate(runnable, initialDelay, period, unit);
  }

  @NotNull
  public ScheduledFuture<?> scheduleWithFixedDelay(@NotNull final Runnable runnable,
      final long initialDelay, final long delay, @NotNull final TimeUnit unit) {
    return mExecutor.scheduleWithFixedDelay(runnable, initialDelay, delay, unit);
  }
}
