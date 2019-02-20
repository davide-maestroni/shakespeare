package dm.shakespeare.concurrent;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import dm.shakespeare.util.CQueue;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 06/06/2018.
 */
class ThrottledExecutorService extends AbstractExecutorService implements QueuedExecutorService {

  private final ExecutorService mExecutor;
  private final int mMaxConcurrency;
  private final Object mMutex = new Object();
  private final CQueue<Runnable> mQueue = new CQueue<Runnable>();
  private final ThrottledRunnable mRunnable = new ThrottledRunnable();

  private int mPendingCount;

  ThrottledExecutorService(@NotNull final ExecutorService executor, final int maxConcurrency) {
    mExecutor = ConstantConditions.notNull("executor", executor);
    mMaxConcurrency = ConstantConditions.positive("maxConcurrency", maxConcurrency);
  }

  public void execute(@NotNull final Runnable command) {
    synchronized (mMutex) {
      mQueue.add(ConstantConditions.notNull("command", command));
      if (mPendingCount >= mMaxConcurrency) {
        return;
      }
      ++mPendingCount;
    }
    mExecutor.execute(mRunnable);
  }

  public void executeNext(@NotNull final Runnable command) {
    synchronized (mMutex) {
      mQueue.addFirst(ConstantConditions.notNull("command", command));
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
      final CQueue<Runnable> queue = mQueue;
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

    @SuppressWarnings("unchecked")
    public void run() {
      final Runnable runnable;
      synchronized (mMutex) {
        runnable = mQueue.poll();
        if (runnable == null) {
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
