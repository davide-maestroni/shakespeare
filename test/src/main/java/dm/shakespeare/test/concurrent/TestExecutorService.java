package dm.shakespeare.test.concurrent;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import dm.shakespeare.util.CQueue;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 02/08/2019.
 */
public class TestExecutorService extends AbstractExecutorService {

  private final CQueue<Runnable> mRunnables = new CQueue<Runnable>();

  private boolean mIsShutdown;

  @SuppressWarnings("ResultOfMethodCallIgnored")
  public int consume(final int maxTasks) {
    ConstantConditions.positive("maxTasks", maxTasks);
    final CQueue<Runnable> runnables = mRunnables;
    int count = 0;
    while ((count < maxTasks) && !runnables.isEmpty()) {
      runnables.removeFirst().run();
      ++count;
    }
    return count;
  }

  public int consumeAll() {
    final CQueue<Runnable> runnables = mRunnables;
    int count = 0;
    while (!runnables.isEmpty()) {
      runnables.removeFirst().run();
      ++count;
    }
    return count;
  }

  public void execute(@NotNull final Runnable runnable) {
    if (mIsShutdown) {
      throw new RejectedExecutionException();
    }
    mRunnables.add(runnable);
  }

  public int getTaskCount() {
    return mRunnables.size();
  }

  public void shutdown() {
    mIsShutdown = true;
    consumeAll();
  }

  @NotNull
  public List<Runnable> shutdownNow() {
    mIsShutdown = true;
    final ArrayList<Runnable> runnables = new ArrayList<Runnable>();
    mRunnables.drainTo(runnables);
    return runnables;
  }

  public boolean isShutdown() {
    return mIsShutdown;
  }

  public boolean isTerminated() {
    return mIsShutdown;
  }

  public boolean awaitTermination(final long timeout, @NotNull final TimeUnit unit) throws
      InterruptedException {
    if (mIsShutdown) {
      return true;
    }
    Thread.sleep(unit.toMillis(timeout));
    return false;
  }
}
