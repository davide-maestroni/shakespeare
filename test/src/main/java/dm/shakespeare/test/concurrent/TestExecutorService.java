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

package dm.shakespeare.test.concurrent;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide-maestroni on 02/08/2019.
 */
public class TestExecutorService extends AbstractExecutorService {

  private final Queue<Runnable> runnables;

  private boolean isShutdown;

  public TestExecutorService() {
    this(new LinkedList<Runnable>());
  }

  public TestExecutorService(@NotNull final Queue<Runnable> queue) {
    runnables = TestConditions.notNull("queue", queue);
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  public int consume(final int maxTasks) {
    TestConditions.positive("maxTasks", maxTasks);
    final Queue<Runnable> runnables = this.runnables;
    int count = 0;
    while ((count < maxTasks) && !runnables.isEmpty()) {
      runnables.remove().run();
      ++count;
    }
    return count;
  }

  public int consumeAll() {
    final Queue<Runnable> runnables = this.runnables;
    int count = 0;
    while (!runnables.isEmpty()) {
      runnables.remove().run();
      ++count;
    }
    return count;
  }

  public void execute(@NotNull final Runnable runnable) {
    if (isShutdown) {
      throw new RejectedExecutionException();
    }
    runnables.add(runnable);
  }

  public int getTaskCount() {
    return runnables.size();
  }

  public void shutdown() {
    isShutdown = true;
    consumeAll();
  }

  @NotNull
  public List<Runnable> shutdownNow() {
    isShutdown = true;
    final ArrayList<Runnable> runnables = new ArrayList<Runnable>(this.runnables);
    this.runnables.clear();
    return runnables;
  }

  public boolean isShutdown() {
    return isShutdown;
  }

  public boolean isTerminated() {
    return isShutdown;
  }

  public boolean awaitTermination(final long timeout, @NotNull final TimeUnit unit) throws
      InterruptedException {
    if (isShutdown) {
      return true;
    }
    Thread.sleep(unit.toMillis(timeout));
    return false;
  }
}
