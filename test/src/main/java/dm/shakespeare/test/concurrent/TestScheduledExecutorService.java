package dm.shakespeare.test.concurrent;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 02/08/2019.
 */
public class TestScheduledExecutorService extends TestExecutorService
    implements ScheduledExecutorService {

  private final LinkedList<AbstractFuture<?>> mFutures = new LinkedList<AbstractFuture<?>>();

  public void advance(final long period, @NotNull final TimeUnit unit) {
    final Iterator<AbstractFuture<?>> iterator = mFutures.iterator();
    final ArrayList<AbstractFuture<?>> executables = new ArrayList<AbstractFuture<?>>();
    while (iterator.hasNext()) {
      final AbstractFuture<?> future = iterator.next();
      future.setTimestamp(future.getTimestamp() + unit.toNanos(period));
      if (future.getDelay(TimeUnit.NANOSECONDS) <= 0) {
        executables.add(future);
        iterator.remove();
      }
    }

    Collections.sort(executables);
    for (final AbstractFuture<?> future : executables) {
      execute(future);
    }
  }

  @NotNull
  public ScheduledFuture<?> schedule(@NotNull final Runnable command, final long delay,
      @NotNull final TimeUnit unit) {
    final RunnableFuture future =
        new RunnableFuture(mFutures, command, AbstractFuture.toTimestampNanos(delay, unit));
    mFutures.add(future);
    return future;
  }

  @NotNull
  public <V> ScheduledFuture<V> schedule(@NotNull final Callable<V> callable, final long delay,
      @NotNull final TimeUnit unit) {
    final CallableFuture<V> future =
        new CallableFuture<V>(callable, AbstractFuture.toTimestampNanos(delay, unit));
    mFutures.add(future);
    return future;
  }

  @NotNull
  public ScheduledFuture<?> scheduleAtFixedRate(@NotNull final Runnable command,
      final long initialDelay, final long period, @NotNull final TimeUnit unit) {
    final RunnableFuture future =
        new RunnableFuture(mFutures, command, AbstractFuture.toTimestampNanos(initialDelay, unit),
            unit.toNanos(ConstantConditions.positive("period", period)));
    mFutures.add(future);
    return future;
  }

  @NotNull
  public ScheduledFuture<?> scheduleWithFixedDelay(@NotNull final Runnable command,
      final long initialDelay, final long delay, @NotNull final TimeUnit unit) {
    final RunnableFuture future =
        new RunnableFuture(mFutures, command, AbstractFuture.toTimestampNanos(initialDelay, unit),
            unit.toNanos(-ConstantConditions.positive("delay", delay)));
    mFutures.add(future);
    return future;
  }
}
