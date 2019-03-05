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
 * Created by davide-maestroni on 06/06/2018.
 */
class TimeoutScheduledExecutorService extends TimeoutExecutorService
    implements ScheduledExecutorService {

  private final ScheduledExecutorService mExecutor;

  TimeoutScheduledExecutorService(@NotNull final ScheduledExecutorService executor,
      final long timeout, @NotNull final TimeUnit timeUnit, final boolean mayInterruptIfRunning) {
    super(executor, timeout, timeUnit, mayInterruptIfRunning);
    mExecutor = executor;
  }

  @NotNull
  public ScheduledFuture<?> schedule(@NotNull final Runnable command, final long delay,
      @NotNull final TimeUnit unit) {
    return timeout(mExecutor.schedule(command, delay, unit));
  }

  @NotNull
  public <V> ScheduledFuture<V> schedule(@NotNull final Callable<V> callable, final long delay,
      @NotNull final TimeUnit unit) {
    return timeout(mExecutor.schedule(callable, delay, unit));
  }

  @NotNull
  public ScheduledFuture<?> scheduleAtFixedRate(@NotNull final Runnable command,
      final long initialDelay, final long period, @NotNull final TimeUnit unit) {
    return timeout(mExecutor.scheduleAtFixedRate(command, initialDelay, period, unit));
  }

  @NotNull
  public ScheduledFuture<?> scheduleWithFixedDelay(@NotNull final Runnable command,
      final long initialDelay, final long delay, @NotNull final TimeUnit unit) {
    return timeout(mExecutor.scheduleWithFixedDelay(command, initialDelay, delay, unit));
  }
}
