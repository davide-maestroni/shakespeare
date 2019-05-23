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
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import dm.shakespeare.actor.Behavior.Agent;
import dm.shakespeare.util.ConstantConditions;

/**
 * Class wrapping an {@code ScheduledExecutorService} so to provide a way to cancel all the
 * submitted tasks.<br>
 * This class instances do not support any blocking method (like
 * {@link ExecutorService#invokeAll(Collection)} or {@link ExecutorService#invokeAny(Collection)}).
 */
class AgentScheduledExecutorService extends AgentExecutorService
    implements ScheduledExecutorService {

  private final ScheduledExecutorService executorService;

  /**
   * Creates a new scheduled executor service wrapping the specified one.
   *
   * @param executorService the executor service to wrap.
   * @param agent           the behavior agent.
   */
  AgentScheduledExecutorService(@NotNull final ScheduledExecutorService executorService,
      @NotNull final Agent agent) {
    super(executorService, agent);
    this.executorService = executorService;
  }

  @NotNull
  public ScheduledFuture<?> schedule(@NotNull final Runnable command, final long delay,
      @NotNull final TimeUnit unit) {
    return wrap(addFuture(executorService.schedule(wrap(command), delay, unit)));
  }

  @NotNull
  public <V> ScheduledFuture<V> schedule(@NotNull final Callable<V> callable, final long delay,
      @NotNull final TimeUnit unit) {
    return wrap(addFuture(executorService.schedule(wrap(callable), delay, unit)));
  }

  @NotNull
  public ScheduledFuture<?> scheduleAtFixedRate(@NotNull final Runnable command,
      final long initialDelay, final long period, @NotNull final TimeUnit unit) {
    return wrap(
        addFuture(executorService.scheduleAtFixedRate(wrap(command), initialDelay, period, unit)));
  }

  @NotNull
  public ScheduledFuture<?> scheduleWithFixedDelay(@NotNull final Runnable command,
      final long initialDelay, final long delay, @NotNull final TimeUnit unit) {
    return wrap(addFuture(
        executorService.scheduleWithFixedDelay(wrap(command), initialDelay, delay, unit)));
  }

  @NotNull
  private <V> ScheduledFuture<V> wrap(@NotNull final ScheduledFuture<V> future) {
    return new AgentScheduledFuture<V>(future);
  }

  private static class AgentScheduledFuture<V> implements ScheduledFuture<V> {

    private final ScheduledFuture<V> future;

    private AgentScheduledFuture(@NotNull final ScheduledFuture<V> future) {
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

    public int compareTo(@NotNull final Delayed delayed) {
      return future.compareTo(delayed);
    }

    public long getDelay(@NotNull final TimeUnit unit) {
      return future.getDelay(unit);
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

      if (!(o instanceof AgentScheduledFuture)) {
        return false;
      }
      final AgentScheduledFuture<?> that = (AgentScheduledFuture<?>) o;
      return future.equals(that.future);
    }
  }
}
