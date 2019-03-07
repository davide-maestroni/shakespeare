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
 * Created by davide-maestroni on 09/24/2018.
 */
class RunnableFuture extends AbstractFuture<Object> {

  private final ExecutorService mExecutor;
  private final long mPeriod;
  private final Runnable mRunnable;

  RunnableFuture(@NotNull final ExecutorService executor, @NotNull final Runnable runnable,
      final long timestamp) {
    this(executor, runnable, timestamp, 0);
  }

  RunnableFuture(@NotNull final ExecutorService executor, @NotNull final Runnable runnable,
      final long timestamp, final long period) {
    super(timestamp);
    mExecutor = ConstantConditions.notNull("executor", executor);
    mRunnable = ConstantConditions.notNull("runnable", runnable);
    mPeriod = period;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (int) (mPeriod ^ (mPeriod >>> 32));
    result = 31 * result + mRunnable.hashCode();
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
    return (mPeriod == that.mPeriod) && mRunnable.equals(that.mRunnable);
  }

  @NotNull
  @SuppressWarnings("unchecked")
  Future<Object> submit() {
    return (Future<Object>) mExecutor.submit(new Runnable() {

      public void run() {
        mRunnable.run();
        updateTimestamp();
      }
    });
  }

  private void updateTimestamp() {
    long period = mPeriod;
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
