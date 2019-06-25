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
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import dm.shakespeare.util.CQueue;
import dm.shakespeare.util.ConstantConditions;

/**
 * Class wrapping an {@code ExecutorService} instance so to limit the number of parallely running
 * tasks to the specified maximum number.
 */
class ThrottledExecutorService extends AbstractExecutorService {

  private final ExecutorService executorService;
  private final int maxConcurrency;
  private final Object mutex = new Object();
  private final CQueue<Runnable> queue = new CQueue<Runnable>();
  private final ThrottledRunnable runnable = new ThrottledRunnable();

  private int pendingCount;

  /**
   * Creates a new executor service wrapping the specified instance.
   *
   * @param executorService the executor service to wrap.
   * @param maxConcurrency  the maximum number of parallel tasks.
   */
  ThrottledExecutorService(@NotNull final ExecutorService executorService,
      final int maxConcurrency) {
    this.executorService = ConstantConditions.notNull("executorService", executorService);
    this.maxConcurrency = ConstantConditions.positive("maxConcurrency", maxConcurrency);
  }

  public void execute(@NotNull final Runnable command) {
    synchronized (mutex) {
      queue.add(ConstantConditions.notNull("command", command));
      if (pendingCount >= maxConcurrency) {
        return;
      }
      ++pendingCount;
    }

    try {
      executorService.execute(runnable);

    } catch (final RejectedExecutionException e) {
      synchronized (mutex) {
        queue.remove(command);
        --pendingCount;
      }
      throw e;
    }
  }

  public void shutdown() {
    executorService.shutdown();
  }

  @NotNull
  public List<Runnable> shutdownNow() {
    executorService.shutdownNow();
    final ArrayList<Runnable> commands;
    synchronized (mutex) {
      final CQueue<Runnable> queue = this.queue;
      commands = new ArrayList<Runnable>(queue);
      queue.clear();
    }
    return commands;
  }

  public boolean isShutdown() {
    return executorService.isShutdown();
  }

  public boolean isTerminated() {
    return executorService.isTerminated();
  }

  public boolean awaitTermination(final long timeout, @NotNull final TimeUnit unit) throws
      InterruptedException {
    return executorService.awaitTermination(timeout, unit);
  }

  void executeNext(@NotNull final Runnable command) {
    synchronized (mutex) {
      queue.addFirst(ConstantConditions.notNull("command", command));
      if (pendingCount >= maxConcurrency) {
        return;
      }
      ++pendingCount;
    }

    try {
      executorService.execute(runnable);

    } catch (final RejectedExecutionException e) {
      synchronized (mutex) {
        queue.remove(command);
        --pendingCount;
      }
      throw e;
    }
  }

  private class ThrottledRunnable implements Runnable {

    public void run() {
      final Runnable command;
      synchronized (mutex) {
        command = queue.poll();
        if (command == null) {
          --pendingCount;
          return;
        }
      }

      try {
        command.run();

      } finally {
        executorService.execute(this);
      }
    }
  }
}
