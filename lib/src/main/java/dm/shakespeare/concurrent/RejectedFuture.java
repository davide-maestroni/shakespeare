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

import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 06/25/2019.
 */
class RejectedFuture<V> implements ScheduledFuture<V> {

  private final RejectedExecutionException exception;

  RejectedFuture(@NotNull final RejectedExecutionException exception) {
    this.exception = ConstantConditions.notNull("exception", exception);
  }

  public boolean cancel(final boolean mayInterruptIfRunning) {
    return false;
  }

  public boolean isCancelled() {
    return false;
  }

  public boolean isDone() {
    return true;
  }

  public V get() throws ExecutionException {
    throw new ExecutionException(exception);
  }

  public V get(final long timeout, @NotNull final TimeUnit timeUnit) throws ExecutionException {
    throw new ExecutionException(exception);
  }

  public int compareTo(@NotNull final Delayed delayed) {
    return 0;
  }

  public long getDelay(@NotNull final TimeUnit timeUnit) {
    return 0;
  }

  @Override
  public int hashCode() {
    return exception.hashCode();
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }

    if ((o == null) || (getClass() != o.getClass())) {
      return false;
    }
    final RejectedFuture<?> that = (RejectedFuture<?>) o;
    return exception.equals(that.exception);
  }
}
