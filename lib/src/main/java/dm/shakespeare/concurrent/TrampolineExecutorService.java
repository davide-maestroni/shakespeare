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

  private final Queue<Runnable> mCommands;
  private final AtomicBoolean mIsRunning = new AtomicBoolean();
  private final AtomicBoolean mIsShutdown = new AtomicBoolean();

  /**
   * Creates a new trampoline executor service.
   */
  TrampolineExecutorService() {
    mCommands = new ConcurrentLinkedQueue<Runnable>();
  }

  /**
   * Creates a new trampoline executor service.
   *
   * @param commandQueue the internal command queue.
   */
  TrampolineExecutorService(@NotNull final BlockingQueue<Runnable> commandQueue) {
    mCommands = ConstantConditions.notNull("commandQueue", commandQueue);
  }

  public void execute(@NotNull final Runnable command) {
    if (mIsShutdown.get()) {
      throw new RejectedExecutionException();
    }
    run(command);
  }

  public void shutdown() {
    mIsShutdown.set(true);
  }

  @NotNull
  public List<Runnable> shutdownNow() {
    mIsShutdown.set(true);
    final ArrayList<Runnable> pending = new ArrayList<Runnable>();
    final Queue<Runnable> commands = mCommands;
    Runnable nextCommand;
    while ((nextCommand = commands.poll()) != null) {
      pending.add(nextCommand);
    }
    commands.clear();
    return pending;
  }

  public boolean isShutdown() {
    return mIsShutdown.get();
  }

  public boolean isTerminated() {
    return mIsShutdown.get() && mCommands.isEmpty();
  }

  public boolean awaitTermination(final long timeout, @NotNull final TimeUnit unit) throws
      InterruptedException {
    final RunnableLatch latch = new RunnableLatch();
    run(latch);
    return latch.await(timeout, unit);
  }

  private void run(@NotNull final Runnable command) {
    final Queue<Runnable> commands = mCommands;
    commands.add(command);
    final AtomicBoolean isRunning = mIsRunning;
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

    private final CountDownLatch mLatch = new CountDownLatch(1);

    public void run() {
      mLatch.countDown();
    }

    boolean await(final long timeout, @NotNull final TimeUnit unit) throws InterruptedException {
      return mLatch.await(timeout, unit);
    }
  }
}
