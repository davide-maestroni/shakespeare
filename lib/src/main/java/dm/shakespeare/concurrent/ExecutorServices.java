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

import java.util.WeakHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import dm.shakespeare.util.ConstantConditions;

/**
 * Utility class providing constructors for several {@link ExecutorService} and
 * {@link ScheduledExecutorService} classes.
 */
public class ExecutorServices {

  private static final WeakHashMap<ExecutorService, ScheduledExecutorService> sScheduledExecutors =
      new WeakHashMap<ExecutorService, ScheduledExecutorService>();

  /**
   * Avoid instantiation.
   */
  private ExecutorServices() {
    ConstantConditions.avoid();
  }

  /**
   * Converts the specified instance into an {@code ActorExecutorService}.<br>
   * If the input parameter is already an {@code ActorExecutorService}, the same object is returned.
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
   * Converts the specified instance into an {@code ActorScheduledExecutorService}.<br>
   * If the input parameter is already an {@code ActorScheduledExecutorService}, the same object
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
   * Converts the specified instance into an {@code ScheduledExecutorService}.<br>
   * If the input parameter is already an {@code ScheduledExecutorService}, the same object is
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
      final WeakHashMap<ExecutorService, ScheduledExecutorService> executors = sScheduledExecutors;
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
   * A local executor is an {@code ExecutorService} implementation maintaining a queue of
   * commands local to each calling thread. It may act as a trampoline of tasks and comply to the
   * {@code ExecutorService} but for its {@code shutdown} methods. In fact, the local executor
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
   * Creates a new trampoline executor service instance.<br>
   * A trampoline executor is an {@link ExecutorService} implementation maintaining a queue of
   * commands which are consumed in the calling threads.
   *
   * @return the executor instance.
   */
  @NotNull
  public static ExecutorService newTrampolineExecutor() {
    return new TrampolineExecutorService();
  }

  /**
   * Creates a new trampoline executor service instance.<br>
   * A trampoline executor is an {@code ExecutorService} implementation maintaining a queue of
   * commands which are consumed in the calling threads.
   *
   * @param commandQueue the internal command queue.
   * @return the executor instance.
   */
  @NotNull
  public static ExecutorService newTrampolineExecutor(
      @NotNull final BlockingQueue<Runnable> commandQueue) {
    return new TrampolineExecutorService(commandQueue);
  }

  /**
   * Wraps the specified {@code ExecutorService} instance so to run the passed tasks with the
   * specified priority.<br>
   * Several prioritizing services can be created from the same instance. Submitted commands will
   * age every time an higher priority one takes the precedence, so that older commands slowly
   * increase their priority. Such mechanism effectively prevents starvation of low priority tasks.
   * <br>
   * When assigning priority to services, it is important to keep in mind that the difference
   * between two priorities corresponds to the maximum age the lower priority commands will have,
   * before getting precedence over the higher priority ones.
   *
   * @param priority        the execution priority.
   * @param executorService the executor service to wrap.
   * @return the prioritizing executor service.
   */
  @NotNull
  public static ExecutorService withPriority(final int priority,
      @NotNull final ExecutorService executorService) {
    if (executorService instanceof ScheduledExecutorService) {
      return withPriority(priority, (ScheduledExecutorService) executorService);
    }
    return new PriorityExecutorService(executorService, priority);
  }

  /**
   * Wraps the specified {@code ScheduledExecutorService} instance so to run the passed tasks
   * with the specified priority.<br>
   * Several prioritizing services can be created from the same instance. Submitted commands will
   * age every time an higher priority one takes the precedence, so that older commands slowly
   * increase their priority. Such mechanism effectively prevents starvation of low priority tasks.
   * <br>
   * When assigning priority to services, it is important to keep in mind that the difference
   * between two priorities corresponds to the maximum age the lower priority commands will have,
   * before getting precedence over the higher priority ones.
   *
   * @param priority        the execution priority.
   * @param executorService the executor service to wrap.
   * @return the prioritizing executor service.
   */
  @NotNull
  public static ScheduledExecutorService withPriority(final int priority,
      @NotNull final ScheduledExecutorService executorService) {
    return new PriorityScheduledExecutorService(executorService, priority);
  }

  /**
   * Wraps the specified {@code ExecutorService} instance so to limit the number of parallely
   * running tasks to the specified maximum number.
   *
   * @param maxConcurrency  the maximum number of parallel tasks.
   * @param executorService the executor service to wrap.
   * @return the throttled executor service.
   */
  @NotNull
  public static ExecutorService withThrottling(final int maxConcurrency,
      @NotNull final ExecutorService executorService) {
    if (executorService instanceof ScheduledExecutorService) {
      return withThrottling(maxConcurrency, (ScheduledExecutorService) executorService);
    }
    return new ThrottledExecutorService(executorService, maxConcurrency);
  }

  /**
   * Wraps the specified {@code ScheduledExecutorService} instance so to limit the number of
   * parallely running tasks to the specified maximum number.
   *
   * @param maxConcurrency  the maximum number of parallel tasks.
   * @param executorService the executor service to wrap.
   * @return the throttled executor service.
   */
  @NotNull
  public static ScheduledExecutorService withThrottling(final int maxConcurrency,
      @NotNull final ScheduledExecutorService executorService) {
    return new ThrottledScheduledExecutorService(executorService, maxConcurrency);
  }

  /**
   * Wraps the specified {@code ExecutorService} instance so to limit the execution time of each
   * submitted task.
   *
   * @param timeout               the execution timeout.
   * @param timeUnit              the timeout unit.
   * @param mayInterruptIfRunning whether to interrupt running tasks when the timeout elapses.
   * @param executorService       the executor service to wrap.
   * @return the timing out executor service.
   */
  @NotNull
  public static ExecutorService withTimeout(final long timeout, @NotNull final TimeUnit timeUnit,
      final boolean mayInterruptIfRunning, @NotNull final ExecutorService executorService) {
    if (executorService instanceof ScheduledExecutorService) {
      return withTimeout(timeout, timeUnit, mayInterruptIfRunning,
          (ScheduledExecutorService) executorService);
    }
    return new TimeoutExecutorService(executorService, timeout, timeUnit, mayInterruptIfRunning);
  }

  /**
   * Wraps the specified {@code ScheduledExecutorService} instance so to limit the execution time
   * of each submitted task.
   *
   * @param timeout               the execution timeout.
   * @param timeUnit              the timeout unit.
   * @param mayInterruptIfRunning whether to interrupt running tasks when the timeout elapses.
   * @param executorService       the executor service to wrap.
   * @return the timing out executor service.
   */
  @NotNull
  public static ScheduledExecutorService withTimeout(final long timeout,
      @NotNull final TimeUnit timeUnit, final boolean mayInterruptIfRunning,
      @NotNull final ScheduledExecutorService executorService) {
    return new TimeoutScheduledExecutorService(executorService, timeout, timeUnit,
        mayInterruptIfRunning);
  }
}
