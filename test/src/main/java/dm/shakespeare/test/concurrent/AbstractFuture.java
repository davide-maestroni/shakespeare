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

package dm.shakespeare.test.concurrent;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CancellationException;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide-maestroni on 02/14/2019.
 */
abstract class AbstractFuture<V> implements ScheduledFuture<V>, Runnable {

  private ExecutionException exception;
  private boolean isCancelled;
  private boolean isDone;
  private long timestamp;
  private V value;

  AbstractFuture(final long timestamp) {
    this.timestamp = timestamp;
  }

  static long toTimestampNanos(final long delay, @NotNull final TimeUnit unit) {
    return TimeUnit.NANOSECONDS.convert(Math.max(0, delay), unit);
  }

  public boolean cancel(final boolean mayInterruptIfRunning) {
    if (!isDone && !isCancelled) {
      isCancelled = true;
      return true;
    }
    return false;
  }

  public boolean isCancelled() {
    return isCancelled;
  }

  public boolean isDone() {
    return isDone;
  }

  public V get() throws InterruptedException, ExecutionException {
    if (isCancelled) {
      throw new CancellationException();
    }
    final ExecutionException exception = this.exception;
    if (exception != null) {
      throw exception;
    }

    if (!isDone) {
      throw new InterruptedException();
    }
    return value;
  }

  public V get(final long timeout, @NotNull final TimeUnit timeUnit) throws InterruptedException,
      ExecutionException {
    if (isCancelled) {
      throw new CancellationException();
    }
    final ExecutionException exception = this.exception;
    if (exception != null) {
      throw exception;
    }

    if (!isDone) {
      throw new InterruptedException();
    }
    return value;
  }

  public int compareTo(@NotNull final Delayed delayed) {
    if (delayed == this) {
      return 0;
    }
    return Long.valueOf(getDelay(TimeUnit.NANOSECONDS))
        .compareTo(delayed.getDelay(TimeUnit.NANOSECONDS));
  }

  public long getDelay(@NotNull final TimeUnit timeUnit) {
    return timeUnit.convert(timestamp, TimeUnit.NANOSECONDS);
  }

  @Override
  public int hashCode() {
    int result = (int) (timestamp ^ (timestamp >>> 32));
    result = 31 * result + (exception != null ? exception.hashCode() : 0);
    result = 31 * result + (isCancelled ? 1 : 0);
    result = 31 * result + (isDone ? 1 : 0);
    result = 31 * result + (value != null ? value.hashCode() : 0);
    return result;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof AbstractFuture)) {
      return false;
    }
    final AbstractFuture<?> that = (AbstractFuture<?>) o;
    return (timestamp == that.timestamp) && (isCancelled == that.isCancelled) && (isDone
        == that.isDone) && (exception != null ? exception.equals(that.exception)
        : that.exception == null) && (value != null ? value.equals(that.value)
        : that.value == null);
  }

  public void run() {
    try {
      value = getValue();

    } catch (final Throwable t) {
      exception = new ExecutionException(t);
      if (t instanceof InterruptedException) {
        Thread.currentThread().interrupt();

      } else if (t instanceof Error) {
        // rethrow errors
        throw (Error) t;
      }

    } finally {
      isDone = true;
    }
  }

  long getTimestamp() {
    return timestamp;
  }

  void setTimestamp(final long timestamp) {
    this.timestamp = timestamp;
  }

  abstract V getValue() throws Exception;
}
