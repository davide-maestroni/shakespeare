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
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import dm.shakespeare.util.TimeUnits;
import dm.shakespeare.util.TimeUnits.Condition;

/**
 * Abstract base implementation of a scheduled future proxying another future.
 *
 * @param <V> the returned result type.
 */
abstract class AbstractFuture<V> implements ScheduledFuture<V>, Runnable {

  private final Object mMutex = new Object();
  private final AtomicLong mTimestamp;

  private Future<V> mFuture;
  private ScheduledFuture<?> mScheduledFuture;

  /**
   * Creates a new future instance to be executed at the specified timestamp.
   *
   * @param timestamp the execution timestamp in number of nanoseconds.
   */
  AbstractFuture(final long timestamp) {
    mTimestamp = new AtomicLong(timestamp);
  }

  public boolean cancel(final boolean mayInterruptIfRunning) {
    final ScheduledFuture<?> scheduledFuture = mScheduledFuture;
    boolean isCancelled =
        (scheduledFuture != null) && scheduledFuture.cancel(mayInterruptIfRunning);
    synchronized (mMutex) {
      final Future<?> future = mFuture;
      if (future != null) {
        isCancelled |= future.cancel(mayInterruptIfRunning);
      }
    }
    return isCancelled;
  }

  public boolean isCancelled() {
    final ScheduledFuture<?> scheduledFuture = mScheduledFuture;
    boolean isCancelled = (scheduledFuture != null) && scheduledFuture.isCancelled();
    synchronized (mMutex) {
      final Future<?> future = mFuture;
      if (future != null) {
        isCancelled |= future.isCancelled();
      }
    }
    return isCancelled;
  }

  public boolean isDone() {
    final ScheduledFuture<?> scheduledFuture = mScheduledFuture;
    boolean isDone = (scheduledFuture != null) && scheduledFuture.isDone();
    if (isDone) {
      synchronized (mMutex) {
        final Future<?> future = mFuture;
        if (future != null) {
          return future.isDone();
        }
      }
    }
    return isDone;
  }

  public V get() throws InterruptedException, ExecutionException {
    synchronized (mMutex) {
      if (TimeUnits.waitUntil(mMutex, new Condition() {

        public boolean isTrue() {
          return (mFuture != null);
        }
      }, -1, TimeUnit.MILLISECONDS)) {
        return mFuture.get();
      }
    }

    throw new IllegalStateException();
  }

  public V get(final long timeout, @NotNull final TimeUnit timeUnit) throws InterruptedException,
      ExecutionException, TimeoutException {
    synchronized (mMutex) {
      final long startTime = System.currentTimeMillis();
      if (TimeUnits.waitUntil(mMutex, new Condition() {

        public boolean isTrue() {
          return (mFuture != null);
        }
      }, timeout, timeUnit)) {
        return mFuture.get(timeUnit.toMillis(timeout) + startTime - System.currentTimeMillis(),
            TimeUnit.MILLISECONDS);
      }
    }

    throw new TimeoutException();
  }

  public int compareTo(@NotNull final Delayed delayed) {
    if (delayed == this) {
      return 0;
    }
    return Long.valueOf(getDelay(TimeUnit.NANOSECONDS))
        .compareTo(delayed.getDelay(TimeUnit.NANOSECONDS));
  }

  public long getDelay(@NotNull final TimeUnit timeUnit) {
    return timeUnit.convert(mTimestamp.get() - System.nanoTime(), TimeUnit.NANOSECONDS);
  }

  @Override
  public int hashCode() {
    int result = mTimestamp.hashCode();
    result = 31 * result + (mFuture != null ? mFuture.hashCode() : 0);
    result = 31 * result + (mScheduledFuture != null ? mScheduledFuture.hashCode() : 0);
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
    return mTimestamp.equals(that.mTimestamp) && (mFuture != null ? mFuture.equals(that.mFuture)
        : that.mFuture == null) && (mScheduledFuture != null ? mScheduledFuture.equals(
        that.mScheduledFuture) : that.mScheduledFuture == null);
  }

  public void run() {
    final Future<V> future = submit();
    synchronized (mMutex) {
      mFuture = future;
      mMutex.notifyAll();
    }
  }

  @NotNull
  AtomicLong getTimestamp() {
    return mTimestamp;
  }

  void setFuture(@NotNull final ScheduledFuture<?> scheduledFuture) {
    mScheduledFuture = scheduledFuture;
  }

  @NotNull
  abstract Future<V> submit();
}
