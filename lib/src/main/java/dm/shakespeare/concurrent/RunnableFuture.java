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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import dm.shakespeare.util.ConstantConditions;
import dm.shakespeare.util.TimeUnits;

/**
 * Scheduled future implementation wrapping a {@link Runnable}.
 */
class RunnableFuture extends AbstractFuture<Object> {

  private final ExecutorService executorService;
  private final long period;
  private final Runnable runnable;

  /**
   * Creates a new future wrapping the specified runnable instance.
   *
   * @param executorService the underlying executor service.
   * @param runnable        the runnable to wrap.
   * @param timestamp       the execution timestamp in number of nanoseconds.
   */
  RunnableFuture(@NotNull final ExecutorService executorService, @NotNull final Runnable runnable,
      final long timestamp) {
    this(executorService, runnable, timestamp, 0);
  }

  /**
   * Creates a new future wrapping the specified runnable instance.
   *
   * @param executorService the underlying executor service.
   * @param runnable        the runnable to wrap.
   * @param timestamp       the execution timestamp in number of nanoseconds.
   * @param period          the execution period in number of nanoseconds.
   */
  RunnableFuture(@NotNull final ExecutorService executorService, @NotNull final Runnable runnable,
      final long timestamp, final long period) {
    super(timestamp);
    this.executorService = ConstantConditions.notNull("executorService", executorService);
    this.runnable = ConstantConditions.notNull("runnable", runnable);
    this.period = period;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (int) (period ^ (period >>> 32));
    result = 31 * result + runnable.hashCode();
    return result;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }

    if ((o == null) || getClass() != o.getClass()) {
      return false;
    }

    if (!super.equals(o)) {
      return false;
    }
    final RunnableFuture that = (RunnableFuture) o;
    return (period == that.period) && runnable.equals(that.runnable);
  }

  @NotNull
  @SuppressWarnings("unchecked")
  Future<Object> submit() {
    return (Future<Object>) executorService.submit(new Runnable() {

      public void run() {
        runnable.run();
        updateTimestamp();
      }
    });
  }

  private void updateTimestamp() {
    long period = this.period;
    if (period == 0) {
      return;
    }

    if (period > 0) {
      getTimestamp().addAndGet(period);

    } else {
      getTimestamp().set(TimeUnits.toTimestampNanos(-period, TimeUnit.NANOSECONDS));
    }
  }
}
