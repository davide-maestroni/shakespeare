package dm.shakespeare2;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import dm.shakespeare.executor.ExecutorServices;
import dm.shakespeare.log.Logger;
import dm.shakespeare.util.ConstantConditions;
import dm.shakespeare2.actor.Actor;
import dm.shakespeare2.actor.Behavior;
import dm.shakespeare2.actor.Behavior.Context;
import dm.shakespeare2.actor.Envelop;
import dm.shakespeare2.actor.Options;
import dm.shakespeare2.message.ActorDismissed;
import dm.shakespeare2.message.Bounce;
import dm.shakespeare2.message.Failure;
import dm.shakespeare2.message.QuotaExceeded;
import dm.shakespeare2.message.Success;

/**
 * Created by davide-maestroni on 01/10/2019.
 */
class LocalContext implements Context {

  private static final QuotaNotifier DUMMY_NOTIFIER = new QuotaNotifier() {

    public void consume() {
    }

    public boolean exceedsQuota(final int size) {
      return false;
    }

    public void quotaExceeded(final Object message, @NotNull final Envelop envelop) {
    }
  };
  private final ContextExecutorService mContextExecutor;
  private final Runnable mDismissRunnable;
  private final ExecutorService mExecutor;
  private final Logger mLogger;
  private final int mQuota;
  private final QuotaNotifier mQuotaNotifier;
  private final Runnable mRestartRunnable;
  private final LocalStage mStage;

  private Actor mActor;
  private Behavior mBehavior;
  private BehaviorWrapper mBehaviorWrapper = new BehaviorStarter();
  private ContextScheduledExecutorService mContextScheduledExecutor;
  private volatile boolean mDismissed;
  private boolean mStopped;

  LocalContext(@NotNull final LocalStage stage, @NotNull final Behavior behavior, final int quota,
      @NotNull final ExecutorService executor, @NotNull final Logger logger) {
    mStage = ConstantConditions.notNull("stage", stage);
    mBehavior = ConstantConditions.notNull("behavior", behavior);
    mExecutor = ExecutorServices.withThrottling(1, executor);
    mLogger = ConstantConditions.notNull("logger", logger);
    mContextExecutor = new ContextExecutorService(executor);
    mQuotaNotifier = ((mQuota = ConstantConditions.positive("quota", quota)) < Integer.MAX_VALUE)
        ? new DefaultQuotaNotifier() : DUMMY_NOTIFIER;
    mDismissRunnable = new Runnable() {

      public void run() {
        mBehaviorWrapper.onStop(LocalContext.this);
      }
    };
    mRestartRunnable = new Runnable() {

      public void run() {
        mBehaviorWrapper.onRestart(LocalContext.this);
      }
    };
  }

  public void dismissSelf() {
    mExecutor.execute(mDismissRunnable);
  }

  @NotNull
  public ExecutorService getExecutor() {
    return mExecutor;
  }

  @NotNull
  public Logger getLogger() {
    return mLogger;
  }

  @NotNull
  public ScheduledExecutorService getScheduledExecutor() {
    if (mContextScheduledExecutor == null) {
      mContextScheduledExecutor =
          new ContextScheduledExecutorService(ExecutorServices.asScheduled(mExecutor));
    }

    return mContextScheduledExecutor;
  }

  @NotNull
  public Actor getSelf() {
    return mActor;
  }

  public void restartSelf() {
    mExecutor.execute(mRestartRunnable);
  }

  public void setBehavior(@NotNull final Behavior behavior) {
    mBehavior = ConstantConditions.notNull("behavior", behavior);
  }

  void dismiss(final boolean mayInterruptIfRunning) {
    mDismissed = true;
    dismissSelf();
  }

  boolean exceedsQuota(final int size) {
    return mQuotaNotifier.exceedsQuota(size);
  }

  boolean isStopped() {
    return mStopped || mDismissed;
  }

  void message(final Object message, @NotNull final Envelop envelop) {
    mBehaviorWrapper.onMessage(message, envelop, this);
  }

  void messages(@NotNull final Iterable<?> messages, @NotNull final Envelop envelop) {
    if (isStopped() && envelop.getOptions().getBounce()) {
      final ArrayList<Object> bounces = new ArrayList<Object>();
      for (final Object message : messages) {
        bounces.add(new Bounce(message, envelop));
      }

      envelop.getSender().tellAll(bounces, null, mActor);
      return;
    }

    for (final Object message : messages) {
      mBehaviorWrapper.onMessage(message, envelop, this);
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

  void setActor(@NotNull final Actor actor) {
    mActor = ConstantConditions.notNull("actor", actor);
  }

  private void cancelTasks() {
    mContextExecutor.cancelAll(true);
    final ContextScheduledExecutorService scheduledExecutor = mContextScheduledExecutor;
    if (scheduledExecutor != null) {
      scheduledExecutor.cancelAll(true);
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
    }
  }

  private class BehaviorWrapper implements Behavior {

    public void onMessage(final Object message, @NotNull final Envelop envelop,
        @NotNull final Context context) {
      final Options options = envelop.getOptions();
      if (isStopped()) {
        if (options.getBounce()) {
          envelop.getSender().tell(new ActorDismissed(message, envelop), null, mActor);
        }
        return;
      }

      mQuotaNotifier.consume();
      try {
        mBehavior.onMessage(message, envelop, context);
        if (options.getSuccess()) {
          envelop.getSender().tell(new Success(message, envelop), null, mActor);
        }

      } catch (final InterruptedException e) {
        throw new RuntimeException(e);

      } catch (final Throwable t) {
        if (options.getFailure()) {
          envelop.getSender().tell(new Failure(message, envelop, t), null, mActor);
        }
      }
    }

    public void onStart(@NotNull final Context context) {
      try {
        mBehavior.onStart(context);

      } catch (final InterruptedException e) {
        throw new RuntimeException(e);

      } catch (final Throwable t) {
        mLogger.wrn(t, "Suppressed exception");
      }
    }

    public void onStop(@NotNull final Context context) {
      if (mStopped) {
        return;
      }

      mStopped = true;
      mStage.removeActor(mActor.getId());
      cancelTasks();
      try {
        mBehavior.onStop(context);

      } catch (final InterruptedException e) {
        throw new RuntimeException(e);

      } catch (final Throwable t) {
        mLogger.wrn(t, "Suppressed exception");
      }
    }

    void onRestart(@NotNull final Context context) {
      cancelTasks();
      final Behavior behavior = mBehavior;
      try {
        behavior.onStop(context);
        behavior.onStart(context);

      } catch (final InterruptedException e) {
        mStopped = true;
        throw new RuntimeException(e);

      } catch (final Throwable t) {
        mStopped = true;
        mLogger.wrn(t, "Suppressed exception");
      }
    }
  }

  private class DefaultQuotaNotifier implements QuotaNotifier {

    private int mCount = 0;

    public void quotaExceeded(final Object message, @NotNull final Envelop envelop) {
      if (envelop.getOptions().getBounce()) {
        envelop.getSender().tell(new QuotaExceeded(message, envelop), null, mActor);
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
