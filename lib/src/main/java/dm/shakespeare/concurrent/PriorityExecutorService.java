/*
 * Copyright 2019 Davide Maestroni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dm.shakespeare.concurrent;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.WeakHashMap;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import dm.shakespeare.util.ConstantConditions;

/**
 * Class wrapping an {@code ExecutorService} instance so to run the passed tasks with the
 * specified priority.<br>
 * Several prioritizing services can be created from the same instance. Submitted commands will
 * age every time an higher priority one takes the precedence, so that older commands slowly
 * increase their priority. Such mechanism effectively prevents starvation of low priority tasks.
 */
class PriorityExecutorService extends AbstractExecutorService {

  private static final PriorityRunnableComparator PRIORITY_RUNNABLE_COMPARATOR =
      new PriorityRunnableComparator();
  private static final WeakHashMap<ExecutorService, PriorityContext> sContexts =
      new WeakHashMap<ExecutorService, PriorityContext>();

  private final PriorityContext mContext;
  private final ExecutorService mExecutorService;
  private final int mPriority;
  private final PriorityRunnable mRunnable = new PriorityRunnable();

  /**
   * Creates a new executor service wrapping the specified instance.
   *
   * @param executorService the executor service to wrap.
   * @param priority        the tasks priority.
   */
  PriorityExecutorService(@NotNull final ExecutorService executorService, final int priority) {
    mExecutorService = ConstantConditions.notNull("executorService", executorService);
    mPriority = priority;
    synchronized (sContexts) {
      final WeakHashMap<ExecutorService, PriorityContext> contexts = sContexts;
      PriorityContext context = contexts.get(executorService);
      if (context == null) {
        context = new PriorityContext();
        contexts.put(executorService, context);
      }
      mContext = context;
    }
  }

  private static int compareLong(final long l1, final long l2) {
    return (l1 < l2) ? -1 : ((l1 == l2) ? 0 : 1);
  }

  public void execute(@NotNull final Runnable command) {
    synchronized (mContext) {
      final PriorityContext context = mContext;
      context.queue.add(
          new WrappedRunnable(ConstantConditions.notNull("command", command), mPriority,
              context.age--));
    }
    mExecutorService.execute(mRunnable);
  }

  public void shutdown() {
    mExecutorService.shutdown();
  }

  @NotNull
  public List<Runnable> shutdownNow() {
    mExecutorService.shutdownNow();
    final ArrayList<Runnable> commands;
    synchronized (mContext) {
      final PriorityQueue<WrappedRunnable> queue = mContext.queue;
      commands = new ArrayList<Runnable>(queue);
      queue.clear();
    }
    return commands;
  }

  public boolean isShutdown() {
    return mExecutorService.isShutdown();
  }

  public boolean isTerminated() {
    return mExecutorService.isTerminated();
  }

  public boolean awaitTermination(final long timeout, @NotNull final TimeUnit unit) throws
      InterruptedException {
    return mExecutorService.awaitTermination(timeout, unit);
  }

  private static class PriorityContext {

    private final PriorityQueue<WrappedRunnable> queue =
        new PriorityQueue<WrappedRunnable>(10, PRIORITY_RUNNABLE_COMPARATOR);

    private long age = Long.MAX_VALUE - Integer.MAX_VALUE;
  }

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

  private static class WrappedRunnable implements Runnable {

    private final long mAge;
    private final int mPriority;
    private final Runnable mRunnable;

    private WrappedRunnable(@NotNull final Runnable runnable, final int priority, final long age) {
      mRunnable = runnable;
      mPriority = priority;
      mAge = age;
    }

    public void run() {
      mRunnable.run();
    }
  }

  private class PriorityRunnable implements Runnable {

    public void run() {
      final Runnable command;
      synchronized (mContext) {
        command = mContext.queue.poll();
      }

      if (command != null) {
        command.run();
      }
    }
  }
}
