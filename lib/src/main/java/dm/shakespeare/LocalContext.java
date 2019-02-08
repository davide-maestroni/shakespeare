package dm.shakespeare;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;

import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Behavior.Context;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.actor.Options;
import dm.shakespeare.concurrent.ExecutorServices;
import dm.shakespeare.concurrent.QueuedExecutorService;
import dm.shakespeare.function.Observer;
import dm.shakespeare.log.Logger;
import dm.shakespeare.message.Bounce;
import dm.shakespeare.message.DeadLetter;
import dm.shakespeare.message.Delivery;
import dm.shakespeare.message.Failure;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 01/10/2019.
 */
class LocalContext implements Context {

  private static final DeadLetter DEAD_LETTER = new DeadLetter();

  private final QueuedExecutorService mActorExecutor;
  private final Logger mLogger;
  private final HashSet<Actor> mObservers = new HashSet<Actor>();
  private final Observer<Actor> mRemover;

  private Actor mActor;
  private Behavior mBehavior;
  private BehaviorWrapper mBehaviorWrapper = new BehaviorStarter();
  private ContextExecutorService mContextExecutor;
  private ContextScheduledExecutorService mContextScheduledExecutor;
  private Runnable mDismissRunnable;
  private volatile boolean mDismissed;
  private Runnable mRestartRunnable;
  private volatile Thread mRunner;
  private boolean mStopped;

  LocalContext(@NotNull final Observer<Actor> remover, @NotNull final Behavior behavior,
      @NotNull final ExecutorService executor, @NotNull final Logger logger) {
    mRemover = ConstantConditions.notNull("remover", remover);
    mBehavior = ConstantConditions.notNull("behavior", behavior);
    mActorExecutor =
        (executor instanceof ScheduledExecutorService) ? ExecutorServices.asActorExecutor(
            (ScheduledExecutorService) executor) : ExecutorServices.asActorExecutor(executor);
    mLogger = ConstantConditions.notNull("logger", logger);
  }

  public void dismissSelf() {
    mDismissed = true;
    if (mDismissRunnable == null) {
      mDismissRunnable = new Runnable() {

        public void run() {
          mBehaviorWrapper.onStop(LocalContext.this);
        }
      };
    }
    mActorExecutor.executeNext(mDismissRunnable);
  }

  @NotNull
  public ExecutorService getExecutor() {
    if (mContextExecutor == null) {
      final QueuedExecutorService actorExecutor = mActorExecutor;
      if (actorExecutor instanceof ScheduledExecutorService) {
        mContextExecutor = (mContextScheduledExecutor =
            new ContextScheduledExecutorService((ScheduledExecutorService) actorExecutor, this));

      } else {
        mContextExecutor = new ContextExecutorService(actorExecutor, this);
      }
    }
    return mContextExecutor;
  }

  @NotNull
  public Logger getLogger() {
    return mLogger;
  }

  @NotNull
  public ScheduledExecutorService getScheduledExecutor() {
    if (mContextScheduledExecutor == null) {
      final QueuedExecutorService actorExecutor = mActorExecutor;
      if (actorExecutor instanceof ScheduledExecutorService) {
        mContextExecutor = (mContextScheduledExecutor =
            new ContextScheduledExecutorService((ScheduledExecutorService) actorExecutor, this));

      } else {
        mContextScheduledExecutor =
            new ContextScheduledExecutorService(ExecutorServices.asScheduled(mActorExecutor), this);
      }
    }
    return mContextScheduledExecutor;
  }

  @NotNull
  public Actor getSelf() {
    return mActor;
  }

  public boolean isDismissed() {
    return mStopped || mDismissed;
  }

  public void restartSelf() {
    if (mRestartRunnable == null) {
      mRestartRunnable = new Runnable() {

        public void run() {
          mRunner = Thread.currentThread();
          try {
            mBehaviorWrapper.onRestart(LocalContext.this);

          } finally {
            mRunner = null;
          }

          if (Thread.currentThread().isInterrupted()) {
            mBehaviorWrapper.onStop(LocalContext.this);
          }
        }
      };
    }
    mActorExecutor.executeNext(mRestartRunnable);
  }

  public void setBehavior(@NotNull final Behavior behavior) {
    mBehavior = ConstantConditions.notNull("behavior", behavior);
  }

  void addObserver(@NotNull final Actor observer) {
    if (mStopped) {
      reply(observer, DEAD_LETTER, null);

    } else {
      mObservers.add(observer);
    }
  }

  void dismiss(final boolean mayInterruptIfRunning) {
    if (mayInterruptIfRunning) {
      final Thread runner = mRunner;
      if (runner != null) {
        runner.interrupt();
      }
    }
    dismissSelf();
  }

  @NotNull
  QueuedExecutorService getActorExecutor() {
    return mActorExecutor;
  }

  void message(final Object message, @NotNull final Envelop envelop) {
    final Options options = envelop.getOptions();
    if (isDismissed() && (options.getReceiptId() != null)) {
      reply(envelop.getSender(), new Bounce(message, options), options.threadOnly());
      return;
    }
    mRunner = Thread.currentThread();
    try {
      mBehaviorWrapper.onMessage(message, envelop, this);

    } finally {
      mRunner = null;
    }

    if (Thread.currentThread().isInterrupted()) {
      mBehaviorWrapper.onStop(this);
    }
  }

  void messages(@NotNull final Iterable<?> messages, @NotNull final Envelop envelop) {
    final Options options = envelop.getOptions();
    if (isDismissed() && (options.getReceiptId() != null)) {
      final ArrayList<Object> bounces = new ArrayList<Object>();
      for (final Object message : messages) {
        bounces.add(new Bounce(message, options));
      }
      replyAll(envelop.getSender(), bounces, options.threadOnly());
      return;
    }
    mRunner = Thread.currentThread();
    try {
      for (final Object message : messages) {
        mBehaviorWrapper.onMessage(message, envelop, this);
      }

    } finally {
      mRunner = null;
    }

    if (Thread.currentThread().isInterrupted()) {
      mBehaviorWrapper.onStop(this);
    }
  }

  void removeObserver(@NotNull final Actor observer) {
    mObservers.remove(observer);
  }

  void reply(@NotNull final Actor actor, final Object message, @Nullable final Options options) {
    try {
      actor.tell(message, options, mActor);

    } catch (final RejectedExecutionException e) {
      mLogger.err(e, "ignoring exception");
    }
  }

  void replyAll(@NotNull final Actor actor, @NotNull final Iterable<?> messages,
      @Nullable final Options options) {
    try {
      actor.tellAll(messages, options, mActor);

    } catch (final RejectedExecutionException e) {
      mLogger.err(e, "ignoring exception");
    }
  }

  void setActor(@NotNull final Actor actor) {
    mActor = ConstantConditions.notNull("actor", actor);
  }

  private void cancelTasks() {
    final ContextExecutorService executor = mContextExecutor;
    if (executor != null) {
      executor.cancelAll(true);
    }
    final ContextScheduledExecutorService scheduledExecutor = mContextScheduledExecutor;
    if (scheduledExecutor != null) {
      scheduledExecutor.cancelAll(true);
    }
  }

  private void setStopped() {
    mStopped = true;
    try {
      mRemover.accept(mActor);

    } catch (final Exception e) {
      // it should never happen
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }

      throw new RuntimeException(e);
    }

    for (final Actor observer : mObservers) {
      reply(observer, DEAD_LETTER, null);
    }
  }

  private class BehaviorStarter extends BehaviorWrapper {

    @Override
    public void onMessage(final Object message, @NotNull final Envelop envelop,
        @NotNull final Context context) {
      mBehaviorWrapper = new BehaviorWrapper();
      super.onStart(context);
      super.onMessage(message, envelop, context);
      if (Thread.currentThread().isInterrupted()) {
        onStop(context);
      }
    }

    @Override
    public void onStop(@NotNull final Context context) {
    }
  }

  private class BehaviorWrapper implements Behavior {

    public void onMessage(final Object message, @NotNull final Envelop envelop,
        @NotNull final Context context) {
      final Options options = envelop.getOptions();
      if (isDismissed()) {
        if (options.getReceiptId() != null) {
          reply(envelop.getSender(), new Bounce(message, options), options.threadOnly());
        }
        return;
      }

      if (options.getReceiptId() != null) {
        try {
          mBehavior.onMessage(message, envelop, context);
          if (!envelop.isPreventReceipt()) {
            reply(envelop.getSender(), new Delivery(message, options), options.threadOnly());
          }

        } catch (final Throwable t) {
          if (!envelop.isPreventReceipt()) {
            reply(envelop.getSender(), new Failure(message, options, t), options.threadOnly());
          }
          onStop(context);
          if (t instanceof InterruptedException) {
            Thread.currentThread().interrupt();
          }
        }

      } else {
        try {
          mBehavior.onMessage(message, envelop, context);

        } catch (final Throwable t) {
          onStop(context);
          if (t instanceof InterruptedException) {
            Thread.currentThread().interrupt();
          }
        }
      }
    }

    public void onStart(@NotNull final Context context) {
      try {
        mBehavior.onStart(context);

      } catch (final Throwable t) {
        setStopped();
        cancelTasks();
        mLogger.wrn(t, "suppressed exception");
        if (t instanceof InterruptedException) {
          Thread.currentThread().interrupt();
        }
      }
    }

    public void onStop(@NotNull final Context context) {
      if (mStopped) {
        return;
      }
      setStopped();
      try {
        mBehavior.onStop(context);

      } catch (final Throwable t) {
        mLogger.wrn(t, "suppressed exception");
        if (t instanceof InterruptedException) {
          Thread.currentThread().interrupt();
        }

      } finally {
        cancelTasks();
      }
    }

    void onRestart(@NotNull final Context context) {
      if (isDismissed()) {
        return;
      }
      final Behavior behavior = mBehavior;
      try {
        behavior.onStop(context);
        behavior.onStart(context);

      } catch (final Throwable t) {
        setStopped();
        cancelTasks();
        mLogger.wrn(t, "suppressed exception");
        if (t instanceof InterruptedException) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }
}
