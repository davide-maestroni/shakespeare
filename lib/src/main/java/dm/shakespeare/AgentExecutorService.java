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

package dm.shakespeare;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import dm.shakespeare.actor.Behavior.Agent;
import dm.shakespeare.util.ConstantConditions;

/**
 * Class wrapping an {@code ExecutorService} so to provide a way to cancel all the submitted tasks.
 * <br>
 * This class instances do not support any blocking method (like
 * {@link ExecutorService#invokeAll(Collection)} or {@link ExecutorService#invokeAny(Collection)}).
 */
class AgentExecutorService implements ExecutorService {

  private final Agent agent;
  private final ExecutorService executorService;
  private final WeakHashMap<Future<?>, Void> tasks = new WeakHashMap<Future<?>, Void>();

  /**
   * Creates a new executor service wrapping the specified one.
   *
   * @param executorService the executor service to wrap.
   * @param agent           the behavior agent.
   */
  AgentExecutorService(@NotNull final ExecutorService executorService, @NotNull final Agent agent) {
    this.executorService = ConstantConditions.notNull("executorService", executorService);
    this.agent = ConstantConditions.notNull("agent", agent);
  }

  public void execute(@NotNull final Runnable command) {
    tasks.put(executorService.submit(command), null);
  }

  public void shutdown() {
    executorService.shutdown();
    tasks.clear();
  }

  @NotNull
  public List<Runnable> shutdownNow() {
    final List<Runnable> commands = executorService.shutdownNow();
    tasks.clear();
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

  @NotNull
  public <T> Future<T> submit(@NotNull final Callable<T> task) {
    return wrap(addFuture(executorService.submit(wrap(task))));
  }

  @NotNull
  public <T> Future<T> submit(@NotNull final Runnable task, final T result) {
    return wrap(addFuture(executorService.submit(wrap(task), result)));
  }

  @NotNull
  public Future<?> submit(@NotNull final Runnable task) {
    return wrap(addFuture(executorService.submit(wrap(task))));
  }

  @NotNull
  public <T> List<Future<T>> invokeAll(@NotNull final Collection<? extends Callable<T>> tasks) {
    return ConstantConditions.unsupported();
  }

  @NotNull
  public <T> List<Future<T>> invokeAll(@NotNull final Collection<? extends Callable<T>> tasks,
      final long timeout, @NotNull final TimeUnit unit) {
    return ConstantConditions.unsupported();
  }

  @NotNull
  public <T> T invokeAny(@NotNull final Collection<? extends Callable<T>> tasks) {
    return ConstantConditions.unsupported();
  }

  public <T> T invokeAny(@NotNull final Collection<? extends Callable<T>> tasks, final long timeout,
      @NotNull final TimeUnit unit) {
    return ConstantConditions.unsupported();
  }

  <V, F extends Future<V>> F addFuture(final F future) {
    if (future != null) {
      tasks.put(future, null);
    }
    return future;
  }

  void cancelAll(final boolean mayInterruptIfRunning) {
    for (final Future<?> future : tasks.keySet()) {
      future.cancel(mayInterruptIfRunning);
    }
    tasks.clear();
  }

  @NotNull
  <V> Callable<V> wrap(@NotNull final Callable<V> task) {
    return new AgentCallable<V>(task, agent);
  }

  @NotNull
  Runnable wrap(@NotNull final Runnable task) {
    return new AgentRunnable(task, agent);
  }

  @NotNull
  private <V> Future<V> wrap(@NotNull final Future<V> future) {
    return new AgentFuture<V>(future);
  }

  private static class AgentCallable<V> implements Callable<V> {

    private final Agent agent;
    private final Callable<V> task;

    private AgentCallable(@NotNull final Callable<V> task, @NotNull final Agent agent) {
      this.task = ConstantConditions.notNull("task", task);
      this.agent = agent;
    }

    public V call() throws Exception {
      if (agent.isDismissed()) {
        throw new IllegalStateException();
      }
      return task.call();
    }
  }

  private static class AgentFuture<V> implements Future<V> {

    private final Future<V> future;

    private AgentFuture(@NotNull final Future<V> future) {
      this.future = ConstantConditions.notNull("future", future);
    }

    public boolean cancel(final boolean mayInterruptIfRunning) {
      return future.cancel(mayInterruptIfRunning);
    }

    public boolean isCancelled() {
      return future.isCancelled();
    }

    public boolean isDone() {
      return future.isDone();
    }

    public V get() {
      return ConstantConditions.unsupported();
    }

    public V get(final long timeout, @NotNull final TimeUnit unit) {
      return ConstantConditions.unsupported();
    }

    @Override
    public int hashCode() {
      return future.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }

      if (!(o instanceof AgentFuture)) {
        return false;
      }
      final AgentFuture<?> that = (AgentFuture<?>) o;
      return future.equals(that.future);
    }
  }

  private static class AgentRunnable implements Runnable {

    private final Agent agent;
    private final Runnable task;

    private AgentRunnable(@NotNull final Runnable task, @NotNull final Agent agent) {
      this.task = ConstantConditions.notNull("task", task);
      this.agent = agent;
    }

    public void run() {
      if (!agent.isDismissed()) {
        task.run();
      }
    }
  }
}
