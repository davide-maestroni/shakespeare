package dm.shakespeare.executor;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import dm.shakespeare.util.ConstantConditions;
import dm.shakespeare.util.WeakIdentityHashMap;

/**
 * Created by davide-maestroni on 06/06/2018.
 */
class PriorityExecutorService extends AbstractExecutorService {

  private static final PriorityRunnableComparator PRIORITY_RUNNABLE_COMPARATOR =
      new PriorityRunnableComparator();

  private static final WeakIdentityHashMap<ExecutorService, PriorityContext> sContexts =
      new WeakIdentityHashMap<ExecutorService, PriorityContext>();

  private final PriorityContext mContext;
  private final ExecutorService mExecutor;
  private final int mPriority;
  private final PriorityRunnable mRunnable = new PriorityRunnable();

  PriorityExecutorService(@NotNull final ExecutorService executor, final int priority) {
    mExecutor = ConstantConditions.notNull("executor", executor);
    mPriority = priority;

    synchronized (sContexts) {
      final WeakIdentityHashMap<ExecutorService, PriorityContext> contexts = sContexts;
      PriorityContext context = contexts.get(executor);
      if (context == null) {
        context = new PriorityContext();
        contexts.put(executor, context);
      }

      mContext = context;
    }
  }

  private static int compareLong(final long l1, final long l2) {
    return (l1 < l2) ? -1 : ((l1 == l2) ? 0 : 1);
  }

  public void execute(@NotNull final Runnable runnable) {
    synchronized (mContext) {
      final PriorityContext context = mContext;
      context.queue.add(
          new WrappedRunnable(ConstantConditions.notNull("runnable", runnable), mPriority,
              context.age--));
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
    synchronized (mContext) {
      final PriorityQueue<WrappedRunnable> queue = mContext.queue;
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

  private static class PriorityContext {

    private final PriorityQueue<WrappedRunnable> queue =
        new PriorityQueue<WrappedRunnable>(10, PRIORITY_RUNNABLE_COMPARATOR);

    private long age = Long.MAX_VALUE - Integer.MAX_VALUE;
  }

  /**
   * Comparator of priority command instances.
   */
  private static class PriorityRunnableComparator
      implements Comparator<WrappedRunnable>, Serializable {

    // Just don't care...
    private static final long serialVersionUID = -1;

    public int compare(final WrappedRunnable e1, final WrappedRunnable e2) {
      final int thisPriority = e1.mPriority;
      final long thisAge = e1.mAge;
      final int thatPriority = e2.mPriority;
      final long thatAge = e2.mAge;
      final int compare = compareLong(thatAge + thatPriority, thisAge + thisPriority);
      return (compare == 0) ? compareLong(thatAge, thisAge) : compare;
    }
  }

  /**
   * Runnable implementation providing a comparison based on priority and the wrapped command
   * age.
   */
  private static class WrappedRunnable implements Runnable {

    private final long mAge;
    private final int mPriority;
    private final Runnable mRunnable;

    /**
     * Constructor.
     *
     * @param runnable the wrapped command.
     * @param priority the command priority.
     * @param age      the command age.
     */
    private WrappedRunnable(@NotNull final Runnable runnable, final int priority, final long age) {
      mRunnable = runnable;
      mPriority = priority;
      mAge = age;
    }

    public void run() {
      mRunnable.run();
    }
  }

  /**
   * Runnable used to dequeue and run pending runnables, when the maximum concurrency count allows
   * it.
   */
  private class PriorityRunnable implements Runnable {

    public void run() {
      final Runnable runnable;
      synchronized (mContext) {
        runnable = mContext.queue.poll();
      }

      if (runnable != null) {
        runnable.run();
      }
    }
  }
}
