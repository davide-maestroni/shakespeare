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
import java.util.concurrent.TimeUnit;

import dm.shakespeare.util.ConstantConditions;

/**
 * Class maintaining a queue of commands which is local to the calling thread.
 * <p>
 * Created by davide-maestroni on 05/28/2018.
 */
class LocalExecutorService extends AbstractExecutorService implements QueuedExecutorService {

  private static final LocalExecutorService sInstance = new LocalExecutorService();

  private final LocalExecutorThreadLocal mLocalExecutor = new LocalExecutorThreadLocal();

  /**
   * Avoid explicit instantiation.
   */
  private LocalExecutorService() {
  }

  @NotNull
  static LocalExecutorService defaultInstance() {
    return sInstance;
  }

  public void execute(@NotNull final Runnable command) {
    mLocalExecutor.get().execute(ConstantConditions.notNull("command", command));
  }

  public void executeNext(@NotNull final Runnable command) {
    mLocalExecutor.get().executeNext(ConstantConditions.notNull("command", command));
  }

  public void shutdown() {
    ConstantConditions.unsupported();
  }

  @NotNull
  public List<Runnable> shutdownNow() {
    return ConstantConditions.unsupported();
  }

  public boolean isShutdown() {
    return false;
  }

  public boolean isTerminated() {
    return false;
  }

  public boolean awaitTermination(final long timeout, @NotNull final TimeUnit unit) throws
      InterruptedException {
    Thread.sleep(unit.toMillis(timeout));
    return false;
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
