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
 * Utility class providing constructors for several {@link ExecutorService} and
 * {@link ScheduledExecutorService} classes.
 */
public class ExecutorServices {

  private static final WeakIdentityHashMap<ExecutorService, ScheduledExecutorService>
      sScheduledExecutors = new WeakIdentityHashMap<ExecutorService, ScheduledExecutorService>();

  /**
   * Avoid instantiation.
   */
  private ExecutorServices() {
    ConstantConditions.avoid();
  }

  /**
   * Converts the specified instance into an {@link ActorExecutorService}.<br>
   * If the input parameter is already an {@link ActorExecutorService}, the same object is returned.
   *
   * @param executorService the executor service to convert.
   * @return the converted instance.
   */
  @NotNull
  public static ActorExecutorService asActorExecutor(
      @NotNull final ExecutorService executorService) {
    if (executorService instanceof ActorExecutorService) {
      return (ActorExecutorService) executorService;
    }
    return new ActorExecutorService(executorService);
  }

  /**
   * Converts the specified instance into an {@link ActorScheduledExecutorService}.<br>
   * If the input parameter is already an {@link ActorScheduledExecutorService}, the same object
   * is returned.
   *
   * @param executorService the executor service to convert.
   * @return the converted instance.
   */
  @NotNull
  public static ActorScheduledExecutorService asActorExecutor(
      @NotNull final ScheduledExecutorService executorService) {
    if (executorService instanceof ActorScheduledExecutorService) {
      return (ActorScheduledExecutorService) executorService;
    }
    return new ActorScheduledExecutorService(executorService);
  }

  /**
   * Converts the specified instance into an {@link ScheduledExecutorService}.<br>
   * If the input parameter is already an {@link ScheduledExecutorService}, the same object is
   * returned.<br>
   * The wrapping objects are cached so that calling this method with the same instance as input
   * will produce the same result.
   *
   * @param executorService the executor service to convert.
   * @return the converted instance.
   */
  @NotNull
  public static ScheduledExecutorService asScheduled(
      @NotNull final ExecutorService executorService) {
    if (executorService instanceof ScheduledExecutorService) {
      return (ScheduledExecutorService) executorService;
    }
    ScheduledExecutorService scheduledExecutor;
    synchronized (sScheduledExecutors) {
      final WeakIdentityHashMap<ExecutorService, ScheduledExecutorService> executors =
          sScheduledExecutors;
      scheduledExecutor = executors.get(executorService);
      if (scheduledExecutor == null) {
        scheduledExecutor = new ScheduledThreadPoolWrapper(executorService);
        executors.put(executorService, scheduledExecutor);
      }
    }
    return scheduledExecutor;
  }

  /**
   * Returns the global local executor instance.<br>
   * A local executor is an {@link ExecutorService} implementation maintaining a queue of
   * commands local to each calling thread. It may act as a trampoline of tasks and comply to the
   * {@link ExecutorService} but for its {@code shutdown} methods. In fact, the local executor
   * instance cannot be stopped and will throw an {@link UnsupportedOperationException} if an
   * attempt is made.
   *
   * @return the local executor instance.
   */
  @NotNull
  public static ExecutorService localExecutor() {
    return LocalExecutorService.defaultInstance();
  }

  /**
   * Creates pre-configured pool of scheduled threads.<br>
   * The service maintains a fixed number of always available threads, while adding new ones till
   * reaching a maximum number. All the threads exceeding the core ones will be automatically
   * stopped if they stay idle for a pre-defined amount of time.
   *
   * @param threadFactory the thread factory.
   * @return the scheduled executor instance.
   */
  @NotNull
  public static ScheduledExecutorService newDynamicScheduledThreadPool(
      @NotNull final ThreadFactory threadFactory) {
    final int processors = Runtime.getRuntime().availableProcessors();
    return newDynamicScheduledThreadPool(Math.max(0, processors - 1),
        Math.max(2, (processors << 1) - 1), 60L, TimeUnit.SECONDS, threadFactory);
  }

  /**
   * Creates a pool of scheduled threads.<br>
   * The service maintains a fixed number of always available threads, while adding new ones till
   * reaching a maximum number. All the threads exceeding the core ones will be automatically
   * stopped if they stay idle for the specified amount of time.
   *
   * @param corePoolSize    the number of threads to keep in the pool, even if they are idle.
   * @param maximumPoolSize the maximum number of threads to allow in the pool.
   * @param keepAliveTime   when the number of threads is greater than the core, this is the
   *                        maximum time that excess idle threads will wait for new tasks before
   *                        terminating.
   * @param keepAliveUnit   the time unit for the keep alive time.
   * @param threadFactory   the thread factory.
   * @return the scheduled executor instance.
   * @throws IllegalArgumentException if one of the following holds:<ul>
   *                                  <li>{@code corePoolSize < 0}</li>
   *                                  <li>{@code maximumPoolSize <= 0}</li>
   *                                  <li>{@code keepAliveTime < 0}</li></ul>
   */
  @NotNull
  public static ScheduledExecutorService newDynamicScheduledThreadPool(final int corePoolSize,
      final int maximumPoolSize, final long keepAliveTime, @NotNull final TimeUnit keepAliveUnit,
      @NotNull final ThreadFactory threadFactory) {
    return new ScheduledThreadPoolWrapper(corePoolSize, maximumPoolSize, keepAliveTime,
        keepAliveUnit, threadFactory);
  }

  /**
   * Creates a pool of scheduled threads with a pre-defined thread factory.<br>
   * The service maintains a fixed number of always available threads, while adding new ones till
   * reaching a maximum number. All the threads exceeding the core ones will be automatically
   * stopped if they stay idle for the specified amount of time.
   *
   * @param corePoolSize    the number of threads to keep in the pool, even if they are idle.
   * @param maximumPoolSize the maximum number of threads to allow in the pool.
   * @param keepAliveTime   when the number of threads is greater than the core, this is the
   *                        maximum time that excess idle threads will wait for new tasks before
   *                        terminating.
   * @param keepAliveUnit   the time unit for the keep alive time.
   * @return the scheduled executor instance.
   * @throws IllegalArgumentException if one of the following holds:<ul>
   *                                  <li>{@code corePoolSize < 0}</li>
   *                                  <li>{@code maximumPoolSize <= 0}</li>
   *                                  <li>{@code keepAliveTime < 0}</li></ul>
   */
  @NotNull
  public static ScheduledExecutorService newDynamicScheduledThreadPool(final int corePoolSize,
      final int maximumPoolSize, final long keepAliveTime, @NotNull final TimeUnit keepAliveUnit) {
    return new ScheduledThreadPoolWrapper(corePoolSize, maximumPoolSize, keepAliveTime,
        keepAliveUnit);
  }

  /**
   * Creates a new trampoline executor service instance.
   *
   * @return the executor instance.
   */
  @NotNull
  public static ExecutorService newTrampolineExecutor() {
    return new TrampolineExecutorService();
  }

  /**
   * Creates a new trampoline executor service instance.
   *
   * @param commandQueue the internal command queue.
   * @return the executor instance.
   */
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
