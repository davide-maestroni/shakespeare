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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import dm.shakespeare.util.ConstantConditions;
import dm.shakespeare.util.WeakIdentityHashMap;

/**
 * Created by davide-maestroni on 06/06/2018.
 */
public class ExecutorServices {

  private static final WeakIdentityHashMap<ExecutorService, ScheduledExecutorService>
      sScheduledExecutors = new WeakIdentityHashMap<ExecutorService, ScheduledExecutorService>();

  private ExecutorServices() {
    ConstantConditions.avoid();
  }

  @NotNull
  public static ActorExecutorService asActorExecutor(@NotNull final ExecutorService executor) {
    if (executor instanceof ActorExecutorService) {
      return (ActorExecutorService) executor;
    }
    return new ActorExecutorService(executor);
  }

  @NotNull
  public static ActorScheduledExecutorService asActorExecutor(
      @NotNull final ScheduledExecutorService executor) {
    if (executor instanceof ActorScheduledExecutorService) {
      return (ActorScheduledExecutorService) executor;
    }
    return new ActorScheduledExecutorService(executor);
  }

  @NotNull
  public static ScheduledExecutorService asScheduled(@NotNull final ExecutorService executor) {
    if (executor instanceof ScheduledExecutorService) {
      return (ScheduledExecutorService) executor;
    }
    ScheduledExecutorService scheduledExecutor;
    synchronized (sScheduledExecutors) {
      final WeakIdentityHashMap<ExecutorService, ScheduledExecutorService> executors =
          sScheduledExecutors;
      scheduledExecutor = executors.get(executor);
      if (scheduledExecutor == null) {
        scheduledExecutor = new ScheduledThreadPoolWrapper(executor);
        executors.put(executor, scheduledExecutor);
      }
    }
    return scheduledExecutor;
  }

  @NotNull
  public static ExecutorService localExecutor() {
    return LocalExecutorService.defaultInstance();
  }

  @NotNull
  public static ScheduledExecutorService newDynamicScheduledThreadPool(
      @NotNull final ThreadFactory threadFactory) {
    final int processors = Runtime.getRuntime().availableProcessors();
    return newDynamicScheduledThreadPool(Math.max(0, processors - 1),
        Math.max(2, (processors << 1) - 1), 60L, TimeUnit.SECONDS, threadFactory);
  }

  @NotNull
  public static ScheduledExecutorService newDynamicScheduledThreadPool(final int corePoolSize,
      final int maximumPoolSize, final long keepAliveTime, @NotNull final TimeUnit keepAliveUnit,
      @NotNull final ThreadFactory threadFactory) {
    return new ScheduledThreadPoolWrapper(corePoolSize, maximumPoolSize, keepAliveTime,
        keepAliveUnit, threadFactory);
  }

  @NotNull
  public static ScheduledExecutorService newDynamicScheduledThreadPool(final int corePoolSize,
      final int maximumPoolSize, final long keepAliveTime, @NotNull final TimeUnit keepAliveUnit) {
    return new ScheduledThreadPoolWrapper(corePoolSize, maximumPoolSize, keepAliveTime,
        keepAliveUnit);
  }

  @NotNull
  public static ExecutorService newTrampolineExecutor() {
    return new TrampolineExecutorService();
  }

  @NotNull
  public static ExecutorService newTrampolineExecutor(
      @NotNull final BlockingQueue<Runnable> commandQueue) {
    return new TrampolineExecutorService(commandQueue);
  }

  @NotNull
  public static ExecutorService withPriority(final int priority,
      @NotNull final ExecutorService executor) {
    if (executor instanceof ScheduledExecutorService) {
      return withPriority(priority, (ScheduledExecutorService) executor);
    }
    return new PriorityExecutorService(executor, priority);
  }

  @NotNull
  public static ScheduledExecutorService withPriority(final int priority,
      @NotNull final ScheduledExecutorService executor) {
    return new PriorityScheduledExecutorService(executor, priority);
  }

  @NotNull
  public static ExecutorService withThrottling(final int maxExecutions,
      @NotNull final ExecutorService executor) {
    if (executor instanceof ScheduledExecutorService) {
      return withThrottling(maxExecutions, (ScheduledExecutorService) executor);
    }
    return new ThrottledExecutorService(executor, maxExecutions);
  }

  @NotNull
  public static ScheduledExecutorService withThrottling(final int maxExecutions,
      @NotNull final ScheduledExecutorService executor) {
    return new ThrottledScheduledExecutorService(executor, maxExecutions);
  }

  @NotNull
  public static ExecutorService withTimeout(final long timeout, @NotNull final TimeUnit timeUnit,
      final boolean mayInterruptIfRunning, @NotNull final ExecutorService executor) {
    if (executor instanceof ScheduledExecutorService) {
      return withTimeout(timeout, timeUnit, mayInterruptIfRunning,
          (ScheduledExecutorService) executor);
    }
    return new TimeoutExecutorService(executor, timeout, timeUnit, mayInterruptIfRunning);
  }

  @NotNull
  public static ScheduledExecutorService withTimeout(final long timeout,
      @NotNull final TimeUnit timeUnit, final boolean mayInterruptIfRunning,
      @NotNull final ScheduledExecutorService executor) {
    return new TimeoutScheduledExecutorService(executor, timeout, timeUnit, mayInterruptIfRunning);
  }
}
