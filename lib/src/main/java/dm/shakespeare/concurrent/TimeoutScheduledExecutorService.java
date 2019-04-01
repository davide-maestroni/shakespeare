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

/**
 * Class wrapping an {@code ScheduledExecutorService} instance so to limit the execution time of
 * each
 * submitted task.
 */
class TimeoutScheduledExecutorService extends TimeoutExecutorService
    implements ScheduledExecutorService {

  private final ScheduledExecutorService mExecutorService;

  /**
   * Creates a new executor service wrapping the specified instance.
   *
   * @param executorService       the executor service to wrap.
   * @param timeout               the execution timeout.
   * @param timeUnit              the execution timeout unit.
   * @param mayInterruptIfRunning whether to interrupt running tasks when the timeout elapses.
   */
  TimeoutScheduledExecutorService(@NotNull final ScheduledExecutorService executorService,
      final long timeout, @NotNull final TimeUnit timeUnit, final boolean mayInterruptIfRunning) {
    super(executorService, timeout, timeUnit, mayInterruptIfRunning);
    mExecutorService = executorService;
  }

  @NotNull
  public ScheduledFuture<?> schedule(@NotNull final Runnable command, final long delay,
      @NotNull final TimeUnit unit) {
    return timeout(mExecutorService.schedule(command, delay, unit));
  }

  @NotNull
  public <V> ScheduledFuture<V> schedule(@NotNull final Callable<V> callable, final long delay,
      @NotNull final TimeUnit unit) {
    return timeout(mExecutorService.schedule(callable, delay, unit));
  }

  @NotNull
  public ScheduledFuture<?> scheduleAtFixedRate(@NotNull final Runnable command,
      final long initialDelay, final long period, @NotNull final TimeUnit unit) {
    return timeout(mExecutorService.scheduleAtFixedRate(command, initialDelay, period, unit));
  }

  @NotNull
  public ScheduledFuture<?> scheduleWithFixedDelay(@NotNull final Runnable command,
      final long initialDelay, final long delay, @NotNull final TimeUnit unit) {
    return timeout(mExecutorService.scheduleWithFixedDelay(command, initialDelay, delay, unit));
  }
}
