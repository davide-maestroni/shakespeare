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
import java.util.concurrent.RejectedExecutionException;
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
  private static final WeakHashMap<ExecutorService, PriorityContext> contexts =
      new WeakHashMap<ExecutorService, PriorityContext>();

  private final PriorityContext context;
  private final ExecutorService executorService;
  private final int priority;
  private final PriorityRunnable runnable = new PriorityRunnable();

  /**
   * Creates a new executor service wrapping the specified instance.
   *
   * @param executorService the executor service to wrap.
   * @param priority        the tasks priority.
   */
  PriorityExecutorService(@NotNull final ExecutorService executorService, final int priority) {
    this.executorService = ConstantConditions.notNull("executorService", executorService);
    this.priority = priority;
    synchronized (contexts) {
      final WeakHashMap<ExecutorService, PriorityContext> contexts =
          PriorityExecutorService.contexts;
      PriorityContext context = contexts.get(executorService);
      if (context == null) {
        context = new PriorityContext();
        contexts.put(executorService, context);
      }
      this.context = context;
    }
  }

  private static int compareLong(final long l1, final long l2) {
    return (l1 < l2) ? -1 : ((l1 == l2) ? 0 : 1);
  }

  public void execute(@NotNull final Runnable command) {
    final WrappedRunnable wrapped;
    synchronized (context) {
      final PriorityContext context = this.context;
      wrapped = new WrappedRunnable(ConstantConditions.notNull("command", command), priority,
          context.age--);
      context.queue.add(wrapped);
    }

    try {
      executorService.execute(runnable);

    } catch (final RejectedExecutionException e) {
      synchronized (context) {
        context.queue.remove(wrapped);
      }
      throw e;
    }
  }

  public void shutdown() {
    executorService.shutdown();
  }

  @NotNull
  public List<Runnable> shutdownNow() {
    executorService.shutdownNow();
    final ArrayList<Runnable> commands;
    synchronized (context) {
      final PriorityQueue<WrappedRunnable> queue = context.queue;
      commands = new ArrayList<Runnable>(queue);
      queue.clear();
    }
    return commands;
  }

  public boolean isShutdown() {
    return executorService.isShutdown();
  }

  public boolean isTerminated() {
    return executorService.isTerminated();
  }

  public boolean awaitTermination(final long timeout, @NotNull final TimeUnit unit) throws
      InterruptedException {
    return executorService.awaitTermination(timeout, unit);
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
      final int thisPriority = e1.priority;
      final long thisAge = e1.age;
      final int thatPriority = e2.priority;
      final long thatAge = e2.age;
      final int compare = compareLong(thatAge + thatPriority, thisAge + thisPriority);
      return (compare == 0) ? compareLong(thatAge, thisAge) : compare;
    }
  }

  private static class WrappedRunnable implements Runnable {

    private final long age;
    private final int priority;
    private final Runnable runnable;

    private WrappedRunnable(@NotNull final Runnable runnable, final int priority, final long age) {
      this.runnable = runnable;
      this.priority = priority;
      this.age = age;
    }

    public void run() {
      runnable.run();
    }
  }

  private class PriorityRunnable implements Runnable {

    public void run() {
      final Runnable command;
      synchronized (context) {
        command = context.queue.poll();
      }

      if (command != null) {
        command.run();
      }
    }
  }
}
