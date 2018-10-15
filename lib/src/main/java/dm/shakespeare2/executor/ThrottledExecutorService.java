package dm.shakespeare2.executor;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import dm.shakespeare2.util.ConstantConditions;
import dm.shakespeare2.util.DoubleQueue;

/**
 * Created by davide-maestroni on 06/06/2018.
 */
class ThrottledExecutorService extends AbstractExecutorService {

  private final ExecutorService mExecutor;
  private final int mMaxConcurrency;
  private final Object mMutex = new Object();
  private final DoubleQueue<Runnable> mQueue = new DoubleQueue<Runnable>();
  private final ThrottledRunnable mRunnable = new ThrottledRunnable();

  private int mPendingCount;

  ThrottledExecutorService(@NotNull final ExecutorService executor, final int maxConcurrency) {
    mExecutor = ConstantConditions.notNull("executor", executor);
    mMaxConcurrency = ConstantConditions.positive("maxConcurrency", maxConcurrency);
  }

  public void execute(@NotNull final Runnable runnable) {
    synchronized (mMutex) {
      mQueue.add(ConstantConditions.notNull("runnable", runnable));
      if (mPendingCount >= mMaxConcurrency) {
        return;
      }

      ++mPendingCount;
    }

    mExecutor.execute(mRunnable);
  }

  public void shutdown() {
    mExecutor.shutdown();
  }

  @NotNull
  public List<Runnable> shutdownNow() {
    mExecutor.shutdownNow();
    final ArrayList<Runnable> runnables;
    synchronized (mMutex) {
      final DoubleQueue<Runnable> queue = mQueue;
      runnables = new ArrayList<Runnable>(queue);
      queue.clear();
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

  /**
   * Runnable used to dequeue and run pending runnables, when the maximum concurrency count allows
   * it.
   */
  private class ThrottledRunnable implements Runnable {

    public void run() {
      final Runnable runnable;
      synchronized (mMutex) {
        try {
          runnable = mQueue.removeFirst();

        } catch (final NoSuchElementException ignored) {
          --mPendingCount;
          return;
        }
      }

      try {
        runnable.run();

      } finally {
        mExecutor.execute(this);
      }
    }
  }
}
