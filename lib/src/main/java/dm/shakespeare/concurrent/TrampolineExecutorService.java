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
import java.util.Queue;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import dm.shakespeare.util.ConstantConditions;

/**
 * Class implementing a {@code ExecutorService} maintaining a queue of commands which are consumed
 * in the calling threads.
 */
class TrampolineExecutorService extends AbstractExecutorService {

  private final Queue<Runnable> commands;
  private final AtomicBoolean isRunning = new AtomicBoolean();
  private final AtomicBoolean isShutdown = new AtomicBoolean();

  /**
   * Creates a new trampoline executor service.
   */
  TrampolineExecutorService() {
    commands = new ConcurrentLinkedQueue<Runnable>();
  }

  /**
   * Creates a new trampoline executor service.
   *
   * @param commandQueue the internal command queue.
   */
  TrampolineExecutorService(@NotNull final BlockingQueue<Runnable> commandQueue) {
    commands = ConstantConditions.notNull("commandQueue", commandQueue);
  }

  public void execute(@NotNull final Runnable command) {
    if (isShutdown.get()) {
      throw new RejectedExecutionException();
    }
    run(command);
  }

  public void shutdown() {
    isShutdown.set(true);
  }

  @NotNull
  public List<Runnable> shutdownNow() {
    isShutdown.set(true);
    final ArrayList<Runnable> pending = new ArrayList<Runnable>();
    final Queue<Runnable> commands = this.commands;
    Runnable nextCommand;
    while ((nextCommand = commands.poll()) != null) {
      pending.add(nextCommand);
    }
    commands.clear();
    return pending;
  }

  public boolean isShutdown() {
    return isShutdown.get();
  }

  public boolean isTerminated() {
    return isShutdown.get() && commands.isEmpty();
  }

  public boolean awaitTermination(final long timeout, @NotNull final TimeUnit unit) throws
      InterruptedException {
    if (isTerminated()) {
      return true;
    }
    final RunnableLatch latch = new RunnableLatch();
    run(latch);
    return latch.await(timeout, unit);
  }

  private void run(@NotNull final Runnable command) {
    final Queue<Runnable> commands = this.commands;
    commands.add(command);
    final AtomicBoolean isRunning = this.isRunning;
    if (!isRunning.getAndSet(true)) {
      try {
        Runnable nextCommand;
        while ((nextCommand = commands.poll()) != null) {
          nextCommand.run();
        }

      } finally {
        isRunning.set(false);
      }
    }
  }

  private static class RunnableLatch implements Runnable {

    private final CountDownLatch latch = new CountDownLatch(1);

    public void run() {
      latch.countDown();
    }

    boolean await(final long timeout, @NotNull final TimeUnit unit) throws InterruptedException {
      return latch.await(timeout, unit);
    }
  }
}
