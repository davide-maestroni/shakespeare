package dm.shakespeare.executor;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 06/06/2018.
 */
class TimeoutExecutorService extends AbstractExecutorService {

  private static final AtomicLong sCount = new AtomicLong();

  private static ScheduledExecutorService sTimeoutService;

  private final ExecutorService mExecutor;
  private final AtomicBoolean mIsShutdown = new AtomicBoolean();
  private final boolean mMayInterruptIfRunning;
  private final TimeUnit mTimeUnit;
  private final long mTimeout;

  TimeoutExecutorService(@NotNull final ExecutorService executor, final long timeout,
      @NotNull final TimeUnit timeUnit, final boolean mayInterruptIfRunning) {
    mExecutor = ConstantConditions.notNull("executor", executor);
    mTimeout = ConstantConditions.positive("timeout", timeout);
    mTimeUnit = ConstantConditions.notNull("timeUnit", timeUnit);
    mMayInterruptIfRunning = mayInterruptIfRunning;
  }

  public void execute(@NotNull final Runnable runnable) {
    timeout(mExecutor.submit(runnable));
  }

  public void shutdown() {
    mExecutor.shutdown();
    if (!mIsShutdown.getAndSet(true)) {
      if (sCount.decrementAndGet() == 0) {
        sTimeoutService.shutdown();
      }
    }
  }

  @NotNull
  public List<Runnable> shutdownNow() {
    final List<Runnable> runnables = mExecutor.shutdownNow();
    if (!mIsShutdown.getAndSet(true)) {
      if (sCount.decrementAndGet() == 0) {
        sTimeoutService.shutdown();
      }
    }

    return runnables;
  }

  public boolean isShutdown() {
    return mExecutor.isShutdown();
  }

  public boolean isTerminated() {
    return mExecutor.isTerminated();
  }

  public boolean awaitTermination(final long timeout, @NotNull final TimeUnit unit) throws
      InterruptedException {
    return mExecutor.awaitTermination(timeout, unit);
  }

  @NotNull
  <F extends Future<?>> F timeout(@NotNull final F future) {
    if (sCount.getAndIncrement() == 0) {
      sTimeoutService = Executors.newSingleThreadScheduledExecutor();
    }

    sTimeoutService.schedule(new CancelRunnable(future, mMayInterruptIfRunning), mTimeout,
        mTimeUnit);
    return future;
  }

  private static class CancelRunnable implements Runnable {

    private final Future<?> mFuture;
    private final boolean mMayInterruptIfRunning;

    private CancelRunnable(@NotNull final Future<?> future, final boolean mayInterruptIfRunning) {
      mFuture = future;
      mMayInterruptIfRunning = mayInterruptIfRunning;
    }

    public void run() {
      mFuture.cancel(mMayInterruptIfRunning);
    }
  }
}
