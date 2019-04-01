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

import dm.shakespeare.actor.Behavior.Context;
import dm.shakespeare.util.ConstantConditions;

/**
 * Class wrapping an {@code ScheduledExecutorService} so to provide a way to cancel all the
 * submitted tasks.<br>
 * This class instances do not support any blocking method (like
 * {@link ExecutorService#invokeAll(Collection)} or {@link ExecutorService#invokeAny(Collection)}).
 */
class ContextScheduledExecutorService extends ContextExecutorService
    implements ScheduledExecutorService {

  private final ScheduledExecutorService mExecutorService;

  /**
   * Creates a new scheduled executor service wrapping the specified one.
   *
   * @param executorService the executor service to wrap.
   * @param context         the behavior context.
   */
  ContextScheduledExecutorService(@NotNull final ScheduledExecutorService executorService,
      @NotNull final Context context) {
    super(executorService, context);
    mExecutorService = executorService;
  }

  @NotNull
  public ScheduledFuture<?> schedule(@NotNull final Runnable command, final long delay,
      @NotNull final TimeUnit unit) {
    return wrap(addFuture(mExecutorService.schedule(wrap(command), delay, unit)));
  }

  @NotNull
  public <V> ScheduledFuture<V> schedule(@NotNull final Callable<V> callable, final long delay,
      @NotNull final TimeUnit unit) {
    return wrap(addFuture(mExecutorService.schedule(wrap(callable), delay, unit)));
  }

  @NotNull
  public ScheduledFuture<?> scheduleAtFixedRate(@NotNull final Runnable command,
      final long initialDelay, final long period, @NotNull final TimeUnit unit) {
    return wrap(
        addFuture(mExecutorService.scheduleAtFixedRate(wrap(command), initialDelay, period, unit)));
  }

  @NotNull
  public ScheduledFuture<?> scheduleWithFixedDelay(@NotNull final Runnable command,
      final long initialDelay, final long delay, @NotNull final TimeUnit unit) {
    return wrap(addFuture(
        mExecutorService.scheduleWithFixedDelay(wrap(command), initialDelay, delay, unit)));
  }

  @NotNull
  private <V> ScheduledFuture<V> wrap(@NotNull final ScheduledFuture<V> future) {
    return new ContextScheduledFuture<V>(future);
  }

  private static class ContextScheduledFuture<V> implements ScheduledFuture<V> {

    private final ScheduledFuture<V> mFuture;

    private ContextScheduledFuture(@NotNull final ScheduledFuture<V> future) {
      mFuture = ConstantConditions.notNull("future", future);
    }

    public boolean cancel(final boolean mayInterruptIfRunning) {
      return mFuture.cancel(mayInterruptIfRunning);
    }

    public boolean isCancelled() {
      return mFuture.isCancelled();
    }

    public boolean isDone() {
      return mFuture.isDone();
    }

    public V get() {
      return ConstantConditions.unsupported();
    }

    public V get(final long timeout, @NotNull final TimeUnit unit) {
      return ConstantConditions.unsupported();
    }

    public int compareTo(@NotNull final Delayed delayed) {
      return mFuture.compareTo(delayed);
    }

    public long getDelay(@NotNull final TimeUnit unit) {
      return mFuture.getDelay(unit);
    }

    @Override
    public int hashCode() {
      return mFuture.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }

      if (!(o instanceof ContextScheduledFuture)) {
        return false;
      }
      final ContextScheduledFuture<?> that = (ContextScheduledFuture<?>) o;
      return mFuture.equals(that.mFuture);
    }
  }
}
