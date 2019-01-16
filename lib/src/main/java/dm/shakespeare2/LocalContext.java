package dm.shakespeare2;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import dm.shakespeare.executor.ExecutorServices;
import dm.shakespeare.executor.QueuedExecutorService;
import dm.shakespeare.log.Logger;
import dm.shakespeare.util.ConstantConditions;
import dm.shakespeare2.actor.Actor;
import dm.shakespeare2.actor.Behavior;
import dm.shakespeare2.actor.Behavior.Context;
import dm.shakespeare2.actor.Envelop;
import dm.shakespeare2.actor.Options;
import dm.shakespeare2.message.ActorDismissed;
import dm.shakespeare2.message.Bounce;
import dm.shakespeare2.message.DeadLetter;
import dm.shakespeare2.message.Failure;
import dm.shakespeare2.message.QuotaExceeded;

/**
 * Created by davide-maestroni on 01/10/2019.
 */
class LocalContext implements Context {

  private static final DeadLetter DEAD_LETTER = new DeadLetter();
  private static final QuotaNotifier DUMMY_NOTIFIER = new QuotaNotifier() {

    public void consume() {
    }

    public boolean exceedsQuota(final int size) {
      return false;
    }

    public void quotaExceeded(final Object message, @NotNull final Envelop envelop) {
    }
  };

  private final QueuedExecutorService mActorExecutor;
  private final Logger mLogger;
  private final HashSet<Actor> mObservers = new HashSet<Actor>();
  private final int mQuota;
  private final QuotaNotifier mQuotaNotifier;
  private final LocalStage mStage;

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

  LocalContext(@NotNull final LocalStage stage, @NotNull final Behavior behavior, final int quota,
      @NotNull final ExecutorService executor, @NotNull final Logger logger) {
    mStage = ConstantConditions.notNull("stage", stage);
    mBehavior = ConstantConditions.notNull("behavior", behavior);
    mActorExecutor =
        (executor instanceof ScheduledExecutorService) ? ExecutorServices.asActorExecutor(
            (ScheduledExecutorService) executor) : ExecutorServices.asActorExecutor(executor);
    mLogger = ConstantConditions.notNull("logger", logger);
    mQuotaNotifier = ((mQuota = ConstantConditions.positive("quota", quota)) < Integer.MAX_VALUE)
        ? new DefaultQuotaNotifier() : DUMMY_NOTIFIER;
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
      observer.tell(DEAD_LETTER, null, mActor);

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

  boolean exceedsQuota(final int size) {
    return mQuotaNotifier.exceedsQuota(size);
  }

  @NotNull
  QueuedExecutorService getActorExecutor() {
    return mActorExecutor;
  }

  void message(final Object message, @NotNull final Envelop envelop) {
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
    if (isDismissed() && options.getBounce()) {
      final ArrayList<Object> bounces = new ArrayList<Object>();
      for (final Object message : messages) {
        bounces.add(new Bounce(message, options));
      }
      envelop.getSender().tellAll(bounces, Options.thread(options.getThread()), mActor);
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

  void quotaExceeded(@NotNull final Iterable<?> messages, @NotNull final Envelop envelop) {
    @SuppressWarnings("UnnecessaryLocalVariable") final QuotaNotifier quotaNotifier =
        mQuotaNotifier;
    for (final Object message : messages) {
      quotaNotifier.quotaExceeded(message, envelop);
    }
  }

  void quotaExceeded(final Object message, @NotNull final Envelop envelop) {
    mQuotaNotifier.quotaExceeded(message, envelop);
  }

  void removeObserver(@NotNull final Actor observer) {
    mObservers.remove(observer);
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
    mStage.removeActor(mActor.getId());
    for (final Actor observer : mObservers) {
      observer.tell(DEAD_LETTER, null, mActor);
    }
  }

  private interface QuotaNotifier {

    void consume();

    boolean exceedsQuota(int size);

    void quotaExceeded(Object message, @NotNull Envelop envelop);
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
        if (options.getBounce()) {
          envelop.getSender()
              .tell(new ActorDismissed(message, options),
                  Options.thread(envelop.getOptions().getThread()), mActor);
        }
        return;
      }

      mQuotaNotifier.consume();
      try {
        mBehavior.onMessage(message, envelop, context);

      } catch (final Throwable t) {
        if (options.getFailure()) {
          envelop.getSender()
              .tell(new Failure(message, options, t),
                  Options.thread(envelop.getOptions().getThread()), mActor);
        }
        onStop(context);
        if (t instanceof InterruptedException) {
          Thread.currentThread().interrupt();
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

  private class DefaultQuotaNotifier implements QuotaNotifier {

    private int mCount = 0;

    public void quotaExceeded(final Object message, @NotNull final Envelop envelop) {
      final Options options = envelop.getOptions();
      if (options.getBounce()) {
        envelop.getSender()
            .tell(new QuotaExceeded(message, options), Options.thread(options.getThread()), mActor);
      }
    }

    public synchronized void consume() {
      --mCount;
    }

    public synchronized boolean exceedsQuota(final int size) {
      if ((mCount + size) > mQuota) {
        return true;
      }
      mCount += size;
      return false;
    }
  }
}
