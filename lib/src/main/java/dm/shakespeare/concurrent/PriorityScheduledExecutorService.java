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

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import dm.shakespeare.util.ConstantConditions;
import dm.shakespeare.util.TimeUnits;

/**
 * Class wrapping an {@code ScheduledExecutorService} instance so to run the passed tasks with the
 * specified priority.<br>
 * Several prioritizing services can be created from the same instance. Submitted commands will
 * age every time an higher priority one takes the precedence, so that older commands slowly
 * increase their priority. Such mechanism effectively prevents starvation of low priority tasks.
 */
class PriorityScheduledExecutorService extends PriorityExecutorService
    implements ScheduledExecutorService {

  private final ScheduledExecutorService executorService;

  /**
   * Creates a new executor service wrapping the specified instance.
   *
   * @param executorService the executor service to wrap.
   * @param priority        the tasks priority.
   */
  PriorityScheduledExecutorService(@NotNull final ScheduledExecutorService executorService,
      final int priority) {
    super(executorService, priority);
    this.executorService = executorService;
  }

  @NotNull
  public ScheduledFuture<?> schedule(@NotNull final Runnable command, final long delay,
      @NotNull final TimeUnit unit) {
    final RunnableFuture future =
        new RunnableFuture(this, command, TimeUnits.toTimestampNanos(delay, unit));
    future.setFuture(executorService.schedule(future, delay, unit));
    return future;
  }

  @NotNull
  public <V> ScheduledFuture<V> schedule(@NotNull final Callable<V> callable, final long delay,
      @NotNull final TimeUnit unit) {
    final CallableFuture<V> future =
        new CallableFuture<V>(this, callable, TimeUnits.toTimestampNanos(delay, unit));
    future.setFuture(executorService.schedule(future, delay, unit));
    return future;
  }

  @NotNull
  public ScheduledFuture<?> scheduleAtFixedRate(@NotNull final Runnable command,
      final long initialDelay, final long period, @NotNull final TimeUnit unit) {
    final RunnableFuture future =
        new RunnableFuture(this, command, TimeUnits.toTimestampNanos(initialDelay, unit),
            unit.toNanos(ConstantConditions.positive("period", period)));
    future.setFuture(executorService.scheduleAtFixedRate(future, initialDelay, period, unit));
    return future;
  }

  @NotNull
  public ScheduledFuture<?> scheduleWithFixedDelay(@NotNull final Runnable command,
      final long initialDelay, final long delay, @NotNull final TimeUnit unit) {
    final RunnableFuture future =
        new RunnableFuture(this, command, TimeUnits.toTimestampNanos(initialDelay, unit),
            unit.toNanos(-ConstantConditions.positive("delay", delay)));
    future.setFuture(executorService.scheduleWithFixedDelay(future, initialDelay, delay, unit));
    return future;
  }
}
