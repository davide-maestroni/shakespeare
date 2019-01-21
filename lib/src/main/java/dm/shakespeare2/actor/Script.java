package dm.shakespeare2.actor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import dm.shakespeare.config.BuildConfig;
import dm.shakespeare.executor.ExecutorServices;
import dm.shakespeare.function.Mapper;
import dm.shakespeare.function.Observer;
import dm.shakespeare.log.LogPrinters;
import dm.shakespeare.log.Logger;
import dm.shakespeare2.actor.Behavior.Context;
import dm.shakespeare2.actor.BehaviorBuilder.Handler;

/**
 * Created by davide-maestroni on 01/08/2019.
 */
public abstract class Script implements Serializable {

  private static final ScheduledExecutorService DEFAULT_EXECUTOR =
      ExecutorServices.newDynamicScheduledThreadPool(new ThreadFactory() {

        private final AtomicLong mCount = new AtomicLong();

        public Thread newThread(@NotNull final Runnable runnable) {
          return new Thread(runnable, "shakespeare-thread-" + mCount.getAndIncrement());
        }
      });

  private static final long serialVersionUID = BuildConfig.VERSION_HASH_CODE;

  @NotNull
  public static <T> Handler<T> acceptHandler(@NotNull final Observer<T> observer) {
    return new AcceptHandler<T>(observer);
  }

  @NotNull
  public static <T> Handler<T> applyHandler(@NotNull final Mapper<T, ?> mapper) {
    return new ApplyHandler<T>(mapper);
  }

  @NotNull
  public static BehaviorBuilder newBehavior() {
    return new DefaultBehaviorBuilder();
  }

  protected static void safeTell(@NotNull final Actor actor, final Object message,
      @Nullable final Options options, @NotNull final Context context) {
    try {
      actor.tell(message, options, context.getSelf());

    } catch (final RejectedExecutionException e) {
      context.getLogger().err(e, "ignoring exception");
    }
  }

  @NotNull
  public abstract Behavior getBehavior(@NotNull String id) throws Exception;

  @NotNull
  public ExecutorService getExecutor(@NotNull final String id) throws Exception {
    return DEFAULT_EXECUTOR;
  }

  @NotNull
  public Logger getLogger(@NotNull final String id) throws Exception {
    return Logger.newLogger(LogPrinters.javaLoggingPrinter(getClass().getName() + "." + id));
  }

  public int getQuota(@NotNull final String id) throws Exception {
    return Integer.MAX_VALUE;
  }
}
