/*
 * Copyright 2018 Davide Maestroni
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

import dm.shakespeare.log.LogPrinters;
import dm.shakespeare.log.Logger;
import dm.shakespeare.util.CQueue;

/**
 * Class maintaining a queue of commands which is local to the calling thread.
 * <p>
 * The implementation ensures that recursive commands are broken into commands handled inside a
 * consuming loop, running in the same thread.
 * <p>
 * Created by davide-maestroni on 09/18/2014.
 */
class LocalExecutor {

  private static final int INITIAL_CAPACITY = 1 << 3;
  private static final LocalExecutorThreadLocal sExecutor = new LocalExecutorThreadLocal();
  private static final Logger sLogger =
      Logger.newLogger(LogPrinters.javaLoggingPrinter(LocalExecutor.class.getName()));

  private final CQueue<Runnable> mCommands = new CQueue<Runnable>(INITIAL_CAPACITY);

  private boolean mIsRunning;

  /**
   * Constructor.
   */
  private LocalExecutor() {
  }

  /**
   * Executes the specified command.
   *
   * @param command the command.
   */
  public static void execute(@NotNull final Runnable command) {
    sExecutor.get().enqueue(command);
  }

  public static void executeNext(@NotNull final Runnable command) {
    sExecutor.get().enqueueNext(command);
  }

  private void enqueue(@NotNull final Runnable command) {
    mCommands.add(command);
    if (!mIsRunning) {
      execute();
    }
  }

  private void enqueueNext(@NotNull final Runnable command) {
    mCommands.addFirst(command);
    if (!mIsRunning) {
      execute();
    }
  }

  private void execute() {
    mIsRunning = true;
    @SuppressWarnings("UnnecessaryLocalVariable") final CQueue<Runnable> commands = mCommands;
    try {
      Runnable command;
      while ((command = commands.poll()) != null) {
        try {
          command.run();

        } catch (final Throwable t) {
          sLogger.wrn(t, "suppressed exception");
          if (Thread.currentThread().isInterrupted()) {
            return;
          }
        }
      }

    } finally {
      mIsRunning = false;
    }
  }

  /**
   * Thread local initializing the queue instance.
   */
  private static class LocalExecutorThreadLocal extends ThreadLocal<LocalExecutor> {

    @Override
    protected LocalExecutor initialValue() {
      return new LocalExecutor();
    }
  }
}
