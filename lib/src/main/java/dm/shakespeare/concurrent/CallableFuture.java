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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import dm.shakespeare.util.ConstantConditions;

/**
 * Scheduled future implementation wrapping a {@link Callable}.
 *
 * @param <V> the returned result type.
 */
class CallableFuture<V> extends AbstractFuture<V> {

  private final Callable<V> mCallable;
  private final ExecutorService mExecutorService;

  /**
   * Creates a new future wrapping the specified callable instance.
   *
   * @param executorService the underlying executor service.
   * @param callable        the callable to wrap.
   * @param timestamp       the execution timestamp in number of nanoseconds.
   */
  CallableFuture(@NotNull final ExecutorService executorService,
      @NotNull final Callable<V> callable, final long timestamp) {
    super(timestamp);
    mExecutorService = ConstantConditions.notNull("executorService", executorService);
    mCallable = ConstantConditions.notNull("callable", callable);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + mCallable.hashCode();
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
    final CallableFuture<?> that = (CallableFuture<?>) o;
    return mCallable.equals(that.mCallable);
  }

  @NotNull
  Future<V> submit() {
    return mExecutorService.submit(mCallable);
  }
}
