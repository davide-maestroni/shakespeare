package dm.shakespeare.executor;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import dm.shakespeare.util.WeakIdentityHashMap;

/**
 * Created by davide-maestroni on 06/06/2018.
 */
public class ExecutorServices {

  private static final WeakIdentityHashMap<ExecutorService, ScheduledExecutorService>
      sScheduledExecutors = new WeakIdentityHashMap<ExecutorService, ScheduledExecutorService>();

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
        scheduledExecutor = new ScheduledThreadPoolExecutorService(executor);
        executors.put(executor, scheduledExecutor);
      }
    }

    return scheduledExecutor;
  }

  @NotNull
  public static ScheduledExecutorService newDynamicScheduledThreadPool(
      @NotNull final ThreadFactory threadFactory) {
    final int processors = Runtime.getRuntime().availableProcessors();
    return newDynamicScheduledThreadPool(Math.max(2, processors >> 1),
        Math.max(2, (processors << 1) - 1), 10L, TimeUnit.SECONDS, threadFactory);
  }

  @NotNull
  public static ScheduledExecutorService newDynamicScheduledThreadPool(final int corePoolSize,
      final int maximumPoolSize, final long keepAliveTime, @NotNull final TimeUnit keepAliveUnit,
      @NotNull final ThreadFactory threadFactory) {
    return new DynamicScheduledThreadPoolExecutorService(corePoolSize, maximumPoolSize,
        keepAliveTime, keepAliveUnit, threadFactory);
  }

  @NotNull
  public static ScheduledExecutorService newDynamicScheduledThreadPool(final int corePoolSize,
      final int maximumPoolSize, final long keepAliveTime, @NotNull final TimeUnit keepAliveUnit) {
    return new DynamicScheduledThreadPoolExecutorService(corePoolSize, maximumPoolSize,
        keepAliveTime, keepAliveUnit);
  }

  @NotNull
  public static ExecutorService trampolineExecutor() {
    return TrampolineExecutor.defaultInstance();
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
