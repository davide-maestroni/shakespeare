package dm.shakespeare2;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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
    return addFuture(mExecutor.schedule(wrap(command), timeout, unit));
  }

  @NotNull
  public <V> ScheduledFuture<V> schedule(@NotNull final Callable<V> task, final long timeout,
      @NotNull final TimeUnit unit) {
    return addFuture(mExecutor.schedule(wrap(task), timeout, unit));
  }

  @NotNull
  public ScheduledFuture<?> scheduleAtFixedRate(@NotNull final Runnable command,
      final long initialDelay, final long period, @NotNull final TimeUnit unit) {
    return addFuture(mExecutor.scheduleAtFixedRate(wrap(command), initialDelay, period, unit));
  }

  @NotNull
  public ScheduledFuture<?> scheduleWithFixedDelay(@NotNull final Runnable command,
      final long initialDelay, final long delay, @NotNull final TimeUnit unit) {
    return addFuture(mExecutor.scheduleWithFixedDelay(wrap(command), initialDelay, delay, unit));
  }
}
