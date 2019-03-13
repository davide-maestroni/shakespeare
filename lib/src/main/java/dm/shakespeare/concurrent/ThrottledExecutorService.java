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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import dm.shakespeare.util.CQueue;
import dm.shakespeare.util.ConstantConditions;

/**
 * Class wrapping an {@link ExecutorService} instance so to limit the number of parallely running
 * tasks to the specified maximum number.
 */
class ThrottledExecutorService extends AbstractExecutorService implements QueuedExecutorService {

  private final ExecutorService mExecutorService;
  private final int mMaxConcurrency;
  private final Object mMutex = new Object();
  private final CQueue<Runnable> mQueue = new CQueue<Runnable>();
  private final ThrottledRunnable mRunnable = new ThrottledRunnable();

  private int mPendingCount;

  /**
   * Creates a new executor service wrapping the specified instance.
   *
   * @param executorService the executor service to wrap.
   * @param maxConcurrency  the maximum number of parallel tasks.
   */
  ThrottledExecutorService(@NotNull final ExecutorService executorService,
      final int maxConcurrency) {
    mExecutorService = ConstantConditions.notNull("executorService", executorService);
    mMaxConcurrency = ConstantConditions.positive("maxConcurrency", maxConcurrency);
  }

  public void execute(@NotNull final Runnable command) {
    synchronized (mMutex) {
      mQueue.add(ConstantConditions.notNull("command", command));
      if (mPendingCount >= mMaxConcurrency) {
        return;
      }
      ++mPendingCount;
    }
    mExecutorService.execute(mRunnable);
  }

  public void executeNext(@NotNull final Runnable command) {
    synchronized (mMutex) {
      mQueue.addFirst(ConstantConditions.notNull("command", command));
      if (mPendingCount >= mMaxConcurrency) {
        return;
      }
      ++mPendingCount;
    }
    mExecutorService.execute(mRunnable);
  }

  public void shutdown() {
    mExecutorService.shutdown();
  }

  @NotNull
  public List<Runnable> shutdownNow() {
    mExecutorService.shutdownNow();
    final ArrayList<Runnable> runnables;
    synchronized (mMutex) {
      final CQueue<Runnable> queue = mQueue;
      runnables = new ArrayList<Runnable>(queue);
      queue.clear();
    }
    return runnables;
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

  private class ThrottledRunnable implements Runnable {

    public void run() {
      final Runnable runnable;
      synchronized (mMutex) {
        runnable = mQueue.poll();
        if (runnable == null) {
          --mPendingCount;
          return;
        }
      }

      try {
        runnable.run();

      } finally {
        mExecutorService.execute(this);
      }
    }
  }
}
