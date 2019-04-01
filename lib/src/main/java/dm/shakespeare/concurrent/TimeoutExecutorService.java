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

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import dm.shakespeare.util.ConstantConditions;

/**
 * Class wrapping an {@code ExecutorService} instance so to limit the execution time of each
 * submitted task.
 */
class TimeoutExecutorService extends AbstractExecutorService {

  private static final Object sMutex = new Object();

  private static long sCount;
  private static ScheduledExecutorService sTimeoutService;

  private final ExecutorService mExecutorService;
  private final AtomicBoolean mIsShutdown = new AtomicBoolean();
  private final boolean mMayInterruptIfRunning;
  private final TimeUnit mTimeUnit;
  private final long mTimeout;

  /**
   * Creates a new executor service wrapping the specified instance.
   *
   * @param executorService       the executor service to wrap.
   * @param timeout               the execution timeout.
   * @param timeUnit              the execution timeout unit.
   * @param mayInterruptIfRunning whether to interrupt running tasks when the timeout elapses.
   */
  TimeoutExecutorService(@NotNull final ExecutorService executorService, final long timeout,
      @NotNull final TimeUnit timeUnit, final boolean mayInterruptIfRunning) {
    mExecutorService = ConstantConditions.notNull("executorService", executorService);
    mTimeout = ConstantConditions.positive("timeout", timeout);
    mTimeUnit = ConstantConditions.notNull("timeUnit", timeUnit);
    mMayInterruptIfRunning = mayInterruptIfRunning;
    startGlobalService();
  }

  private static void shutdownGlobalService() {
    synchronized (sMutex) {
      if (--sCount == 0) {
        sTimeoutService.shutdown();
      }
    }
  }

  private static void startGlobalService() {
    synchronized (sMutex) {
      if (sCount++ == 0) {
        sTimeoutService = Executors.newSingleThreadScheduledExecutor();
      }
    }
  }

  public void execute(@NotNull final Runnable command) {
    timeout(mExecutorService.submit(command));
  }

  public void shutdown() {
    mExecutorService.shutdown();
    if (!mIsShutdown.getAndSet(true)) {
      shutdownGlobalService();
    }
  }

  @NotNull
  public List<Runnable> shutdownNow() {
    final List<Runnable> commands = mExecutorService.shutdownNow();
    if (!mIsShutdown.getAndSet(true)) {
      shutdownGlobalService();
    }
    return commands;
  }

  public boolean isShutdown() {
    return mExecutorService.isShutdown();
  }

  public boolean isTerminated() {
    return mExecutorService.isTerminated();
  }

  public boolean awaitTermination(final long timeout, @NotNull final TimeUnit unit) throws
      InterruptedException {
    return mExecutorService.awaitTermination(timeout, unit);
  }

  @NotNull
  <F extends Future<?>> F timeout(@NotNull final F future) {
    sTimeoutService.schedule(new CancelRunnable(future, mMayInterruptIfRunning), mTimeout,
        mTimeUnit);
    return future;
  }

  private static class CancelRunnable implements Runnable {

    private final Future<?> mFuture;
    private final boolean mMayInterruptIfRunning;

    private CancelRunnable(@NotNull final Future<?> future, final boolean mayInterruptIfRunning) {
      mFuture = future;
      mMayInterruptIfRunning = mayInterruptIfRunning;
    }

    public void run() {
      mFuture.cancel(mMayInterruptIfRunning);
    }
  }
}
