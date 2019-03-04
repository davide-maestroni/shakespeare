package dm.shakespeare.test.concurrent;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide-maestroni on 02/08/2019.
 */
public class TestExecutorService extends AbstractExecutorService {

  private final Queue<Runnable> mRunnables;

  private boolean mIsShutdown;

  public TestExecutorService() {
    this(new LinkedList<Runnable>());
  }

  public TestExecutorService(@NotNull final Queue<Runnable> queue) {
    mRunnables = TestConditions.notNull("queue", queue);
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  public int consume(final int maxTasks) {
    TestConditions.positive("maxTasks", maxTasks);
    final Queue<Runnable> runnables = mRunnables;
    int count = 0;
    while ((count < maxTasks) && !runnables.isEmpty()) {
      runnables.remove().run();
      ++count;
    }
    return count;
  }

  public int consumeAll() {
    final Queue<Runnable> runnables = mRunnables;
    int count = 0;
    while (!runnables.isEmpty()) {
      runnables.remove().run();
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
    final ArrayList<Runnable> runnables = new ArrayList<Runnable>(mRunnables);
    mRunnables.clear();
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
