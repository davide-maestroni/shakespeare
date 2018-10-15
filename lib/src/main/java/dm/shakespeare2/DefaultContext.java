package dm.shakespeare2;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import dm.shakespeare2.actor.Actor;
import dm.shakespeare2.actor.Actor.Conversation;
import dm.shakespeare2.actor.Actor.Envelop;
import dm.shakespeare2.actor.Behavior;
import dm.shakespeare2.actor.Behavior.Context;
import dm.shakespeare2.actor.SignalingMessage;
import dm.shakespeare2.actor.Stage;
import dm.shakespeare2.executor.ExecutorServices;
import dm.shakespeare2.function.Provider;
import dm.shakespeare2.log.Logger;
import dm.shakespeare2.message.ActorStoppedMessage;
import dm.shakespeare2.message.AddActorMonitorMessage;
import dm.shakespeare2.message.QuotaExceededMessage;
import dm.shakespeare2.message.RemoveActorMonitorMessage;
import dm.shakespeare2.message.SetSupervisorMessage;
import dm.shakespeare2.message.SupervisedFailureMessage;
import dm.shakespeare2.message.SupervisedRecoveryMessage;
import dm.shakespeare2.message.SupervisedRecoveryMessage.RecoveryType;
import dm.shakespeare2.message.ThreadAbortedMessage;
import dm.shakespeare2.message.ThreadClosedMessage;
import dm.shakespeare2.message.ThreadFailureMessage;
import dm.shakespeare2.message.ThreadOpenedMessage;
import dm.shakespeare2.message.UnsetSupervisorMessage;
import dm.shakespeare2.util.ConstantConditions;
import dm.shakespeare2.util.DoubleQueue;

/**
 * Created by davide-maestroni on 06/14/2018.
 */
class DefaultContext implements Context {

  private static final SignalingMessage DUMMY_MESSAGE = new SignalingMessage() {};
  private static final QuotaNotifier DUMMY_NOTIFIER = new QuotaNotifier() {

    public void consume() {
    }

    public boolean exceedsQuota(final int size) {
      return false;
    }

    public void quotaExceeded(final Object message, @NotNull final Envelop envelop) {
    }
  };
  private static final HashMap<Class<?>, SignalingMessageHandler> SIGNALING_HANDLERS =
      new HashMap<Class<?>, SignalingMessageHandler>() {{
        put(ThreadOpenedMessage.class, new SignalingMessageHandler() {

          @SuppressWarnings("unchecked")
          public void handle(final Object message, @NotNull final Envelop envelop,
              @NotNull final DefaultContext context) {
            context.mConversationNotifier.open((ThreadOpenedMessage) message, envelop);
          }
        });
        put(ThreadClosedMessage.class, new SignalingMessageHandler() {

          @SuppressWarnings("unchecked")
          public void handle(final Object message, @NotNull final Envelop envelop,
              @NotNull final DefaultContext context) {
            context.mConversationNotifier.close((ThreadClosedMessage) message, envelop);
          }
        });
        put(AddActorMonitorMessage.class, new SignalingMessageHandler() {

          public void handle(final Object message, @NotNull final Envelop envelop,
              @NotNull final DefaultContext context) {
            if (envelop.getSender().equals(context.mActor)) {
              context.mConversationNotifier.fail(message, envelop,
                  new IllegalArgumentException("an actor can't monitor itself"));
              return;
            }

            context.mMonitorNotifier.addMonitor(envelop.getSender());
          }
        });
        put(RemoveActorMonitorMessage.class, new SignalingMessageHandler() {

          public void handle(final Object message, @NotNull final Envelop envelop,
              @NotNull final DefaultContext context) {
            context.mMonitorNotifier.removeMonitor(envelop.getSender());
          }
        });
        put(SetSupervisorMessage.class, new SignalingMessageHandler() {

          public void handle(final Object message, @NotNull final Envelop envelop,
              @NotNull final DefaultContext context) {
            if (envelop.getSender().equals(context.mActor)) {
              context.mConversationNotifier.fail(message, envelop,
                  new IllegalArgumentException("an actor can't supervise itself"));
              return;
            }

            context.mSupervisor = envelop.getSender();
          }
        });
        put(UnsetSupervisorMessage.class, new SignalingMessageHandler() {

          public void handle(final Object message, @NotNull final Envelop envelop,
              @NotNull final DefaultContext context) {
            if (envelop.getSender().equals(context.mSupervisor)) {
              context.mSupervisor = null;
            }
          }
        });
      }};
  private static final List<Class<ThreadFailureMessage>> SUPERVISOR_INCLUDED_MESSAGES =
      Collections.singletonList(ThreadFailureMessage.class);
  private static final HashMap<Class<?>, SignalingMessageHandler> SUPERVISE_HANDLERS =
      new HashMap<Class<?>, SignalingMessageHandler>() {{
        put(SetSupervisorMessage.class, new SignalingMessageHandler() {

          public void handle(final Object message, @NotNull final Envelop envelop,
              @NotNull final DefaultContext context) {
            final Actor actor = context.mActor;
            final Actor sender = envelop.getSender();
            if (sender.equals(actor)) {
              context.mConversationNotifier.fail(message, envelop,
                  new IllegalArgumentException("an actor can't supervise itself"));
              return;
            }

            context.mFailureConversation.close();
            context.mSupervisor = sender;
            final String failureId = context.mFailureId;
            context.mFailureConversation =
                sender.thread(failureId, actor, SUPERVISOR_INCLUDED_MESSAGES)
                    .tell(new SupervisedFailureMessage(failureId, context.mFailure));
          }
        });
        put(UnsetSupervisorMessage.class, new SignalingMessageHandler() {

          public void handle(final Object message, @NotNull final Envelop envelop,
              @NotNull final DefaultContext context) {
            if (envelop.getSender().equals(context.mSupervisor)) {
              context.mFailureConversation.close();
              context.mSupervisor = null;
              context.mHandler = context.new StoppedHandler();
              context.discardDelayed();
              context.stopSelf();
            }
          }
        });
      }};

  private final Actor mActor;
  private final Provider<? extends Behavior> mBehaviorProvider;
  private final BehaviorWrapper mBehaviorWrapper = new BehaviorWrapper();
  private final ConversationNotifier mConversationNotifier;
  private final ExecutorService mExecutor;
  private final Logger mLogger;
  private final boolean mMayInterruptIfRunning;
  private final MonitorNotifier mMonitorNotifier;
  private final int mQuota;
  private final QuotaNotifier mQuotaNotifier;
  private final Runnable mRestartRunnable;
  private final Map<Class<?>, SignalingMessageHandler> mSignalingHandlers;
  private final DefaultStage mStage;
  private final Runnable mStartRunnable;
  private final Runnable mStopRunnable;
  private final Map<Class<?>, SignalingMessageHandler> mSuperviseHandlers = SUPERVISE_HANDLERS;

  private volatile boolean mAborted;
  private Behavior mBehavior;
  private DoubleQueue<DelayedMessage> mDelayedMessages = new DoubleQueue<DelayedMessage>();
  private Throwable mFailure;
  private Conversation<Object> mFailureConversation;
  private String mFailureId;
  private MessageHandler mHandler;
  private Behavior mOriginalBehavior;
  private volatile ScheduledExecutorService mScheduledExecutor;
  private boolean mStopped;
  private Actor mSupervisor;

  DefaultContext(@NotNull final DefaultStage stage, @NotNull final Actor actor,
      @NotNull final Provider<? extends Behavior> provider, final boolean mayInterruptIfRunning,
      final boolean preventDefault, final int quota, @NotNull final ExecutorService executor,
      @NotNull final Logger logger) {
    mStage = ConstantConditions.notNull("stage", stage);
    mActor = ConstantConditions.notNull("actor", actor);
    mBehaviorProvider = ConstantConditions.notNull("provider", provider);
    mExecutor = setupExecutor(executor, mayInterruptIfRunning);
    mLogger = ConstantConditions.notNull("logger", logger);
    mMayInterruptIfRunning = mayInterruptIfRunning;
    mSignalingHandlers =
        (preventDefault) ? Collections.<Class<?>, SignalingMessageHandler>emptyMap()
            : SIGNALING_HANDLERS;
    mHandler = new DefaultHandler();
    mConversationNotifier = new ConversationNotifier(actor);
    mMonitorNotifier = new MonitorNotifier(actor);
    mQuotaNotifier = ((mQuota = ConstantConditions.positive("quota", quota)) < Integer.MAX_VALUE)
        ? new DefaultQuotaNotifier() : DUMMY_NOTIFIER;
    mStartRunnable = new Runnable() {

      public void run() {
        try {
          mBehavior =
              mOriginalBehavior = ConstantConditions.notNull("behavior", mBehaviorProvider.get());
          mBehaviorWrapper.start(DefaultContext.this);

        } catch (final RuntimeException e) {
          mStopped = true;
          throw e;

        } catch (final Exception e) {
          mStopped = true;
          throw new RuntimeException(e);
        }
      }
    };
    mStopRunnable = new Runnable() {

      public void run() {
        mBehaviorWrapper.stop(DefaultContext.this);
      }
    };
    mRestartRunnable = new Runnable() {

      public void run() {
        mHandler = new DefaultHandler();
        final BehaviorWrapper behaviorWrapper = mBehaviorWrapper;
        try {
          behaviorWrapper.stop(DefaultContext.this);
          mBehavior = ConstantConditions.notNull("behavior", mBehaviorProvider.get());
          behaviorWrapper.start(DefaultContext.this);

        } catch (final RuntimeException e) {
          mStopped = true;
          throw e;

        } catch (final Exception e) {
          mStopped = true;
          throw new RuntimeException(e);
        }
      }
    };
  }

  @NotNull
  private static ExecutorService setupExecutor(@NotNull final ExecutorService executor,
      final boolean mayInterruptIfRunning) {
    final ExecutorService executorService = ExecutorServices.withThrottling(1, executor);
    if (mayInterruptIfRunning) {
      if (executorService instanceof ScheduledExecutorService) {
        return new CancellableScheduledExecutorService((ScheduledExecutorService) executorService);
      }

      return new CancellableExecutorService(executorService);
    }

    return executorService;
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
    if (mScheduledExecutor == null) {
      mScheduledExecutor = ExecutorServices.asScheduled(mExecutor);
    }

    return mScheduledExecutor;
  }

  @NotNull
  public Actor getSelf() {
    return mActor;
  }

  @NotNull
  public Stage getStage() {
    return mStage;
  }

  public void resetBehavior() {
    mBehavior = mOriginalBehavior;
  }

  public void restart() {
    mExecutor.execute(mRestartRunnable);
  }

  public void setBehavior(@NotNull final Behavior behavior) {
    mBehavior = ConstantConditions.notNull("behavior", behavior);
  }

  public void stopSelf() {
    mExecutor.execute(mStopRunnable);
  }

  @SuppressWarnings("unchecked")
  void abort() {
    mAborted = true;
    if (mMayInterruptIfRunning) {
      ((CancellableExecutorService) mExecutor).cancel();
    }

    stopSelf();
  }

  void bounce(@NotNull final Iterable<?> messages, @NotNull final Envelop envelop) {
    for (final Object message : messages) {
      bounce(message, envelop);
    }
  }

  void bounce(final Object message, @NotNull final Envelop envelop) {
    if (message instanceof ThreadOpenedMessage) {
      envelop.getSender().tell(ThreadAbortedMessage.defaultInstance(), mActor);
    }

    if (message instanceof AddActorMonitorMessage) {
      envelop.getSender().tell(ActorStoppedMessage.defaultInstance(), mActor);
    }
  }

  boolean exceedsQuota(final int size) {
    return mQuotaNotifier.exceedsQuota(size);
  }

  boolean isStopped() {
    return mStopped || mAborted;
  }

  void message(final Object message, @NotNull final Envelop envelop) {
    mBehaviorWrapper.message(message, envelop, this);
  }

  void quotaExceeded(final Object message, @NotNull final Envelop envelop) {
    mQuotaNotifier.quotaExceeded(message, envelop);
  }

  void quotaExceeded(@NotNull final Iterable<?> messages, @NotNull final Envelop envelop) {
    @SuppressWarnings("UnnecessaryLocalVariable") final QuotaNotifier quotaNotifier =
        mQuotaNotifier;
    for (final Object message : messages) {
      quotaNotifier.quotaExceeded(message, envelop);
    }
  }

  void start() {
    mExecutor.execute(mStartRunnable);
  }

  private void discardDelayed() {
    final Throwable failure = mFailure;
    @SuppressWarnings("UnnecessaryLocalVariable") final ConversationNotifier conversationNotifier =
        mConversationNotifier;
    final DoubleQueue<DelayedMessage> delayedMessages = mDelayedMessages;
    for (final DelayedMessage delayedMessage : delayedMessages) {
      conversationNotifier.discard(delayedMessage.getMessage(), delayedMessage.getEnvelop(),
          failure);
    }

    mDelayedMessages = new DoubleQueue<DelayedMessage>();
  }

  private boolean isFailureAborted(final Object message) {
    return (message instanceof ThreadFailureMessage) || (message instanceof ThreadAbortedMessage);
  }

  private boolean isFailureRecovery(final Object message) {
    return (message instanceof SupervisedRecoveryMessage) && mFailureId.equals(
        ((SupervisedRecoveryMessage) message).getFailureId());
  }

  private void resumeDelayed() {
    final Actor actor = mActor;
    final int size = mDelayedMessages.size();
    for (int i = 0; i < size; ++i) {
      actor.tell(DUMMY_MESSAGE, actor);
    }
  }

  private interface MessageHandler {

    void handle(Object message, @NotNull Envelop envelop);
  }

  private interface QuotaNotifier {

    void consume();

    boolean exceedsQuota(int size);

    void quotaExceeded(Object message, @NotNull Envelop envelop);
  }

  private interface SignalingMessageHandler {

    void handle(Object message, @NotNull Envelop envelop, @NotNull DefaultContext context);
  }

  private static class DelayedMessage {

    private final Envelop mEnvelop;
    private final Object mMessage;

    private DelayedMessage(final Object message, @NotNull final Envelop envelop) {
      mMessage = message;
      mEnvelop = envelop;
    }

    @NotNull
    Envelop getEnvelop() {
      return mEnvelop;
    }

    Object getMessage() {
      return mMessage;
    }
  }

  private class BehaviorWrapper implements Behavior {

    public void message(final Object message, @NotNull final Envelop envelop,
        @NotNull final Context context) {
      if (isStopped()) {
        bounce(message, envelop);
        return;
      }

      mQuotaNotifier.consume();
      mHandler.handle(message, envelop);
    }

    public void start(@NotNull final Context context) throws Exception {
      mBehavior.start(context);
      mMonitorNotifier.start();
    }

    public void stop(@NotNull final Context context) {
      if (mStopped) {
        return;
      }

      try {
        mBehavior.stop(context);

      } catch (final InterruptedException e) {
        throw new RuntimeException(e);

      } catch (final Throwable t) {
        mLogger.wrn(t, "Suppressed exception");

      } finally {
        mStopped = true;
        mStage.removeActor(mActor.getId());
        mConversationNotifier.abort();
        mMonitorNotifier.stop();
      }
    }
  }

  private class DefaultHandler implements MessageHandler {

    public void handle(final Object message, @NotNull final Envelop envelop) {
      if (message != null) {
        if (message == DUMMY_MESSAGE) {
          return;
        }

        final SignalingMessageHandler handler = mSignalingHandlers.get(message.getClass());
        if (handler != null) {
          handler.handle(message, envelop, DefaultContext.this);
          return;
        }
      }

      final ConversationNotifier conversationNotifier = mConversationNotifier;
      try {
        conversationNotifier.receive(message, envelop);
        mBehavior.message(message, envelop, DefaultContext.this);
        conversationNotifier.complete(message, envelop);

      } catch (final Throwable t) {
        mLogger.err(t, "message error");
        conversationNotifier.fail(message, envelop, t);

        final Actor supervisor = mSupervisor;
        if (supervisor != null) {
          try {
            final String failureId = mFailureId = Integer.toString(System.identityHashCode(t));
            mFailureConversation =
                supervisor.thread(failureId, mActor, SUPERVISOR_INCLUDED_MESSAGES)
                    .tell(new SupervisedFailureMessage(failureId, t));
            mFailure = t;
            mHandler = new SuspendedHandler();
            mMonitorNotifier.suspend();
            return;

          } catch (final Throwable ignored) {
          }
        }

        stopSelf();
      }
    }
  }

  private class DefaultQuotaNotifier implements QuotaNotifier {

    private final AtomicInteger mCount = new AtomicInteger();

    public void quotaExceeded(final Object message, @NotNull final Envelop envelop) {
      bounce(message, envelop);
      envelop.getSender().tell(new QuotaExceededMessage(message, envelop), mActor);
    }

    public void consume() {
      mCount.decrementAndGet();
    }

    public boolean exceedsQuota(final int size) {
      final boolean exceeded = (mCount.addAndGet(size) > mQuota);
      if (exceeded) {
        mCount.addAndGet(-size);
      }

      return exceeded;
    }
  }

  private class ResumedHandler extends DefaultHandler {

    public void handle(final Object message, @NotNull final Envelop envelop) {
      final DoubleQueue<DelayedMessage> delayedMessages = mDelayedMessages;
      if (!delayedMessages.isEmpty()) {
        final DelayedMessage delayedMessage = delayedMessages.removeFirst();
        if (message != DUMMY_MESSAGE) {
          delayedMessages.add(new DelayedMessage(message, envelop));
        }

        super.handle(delayedMessage.getMessage(), delayedMessage.getEnvelop());
        return;
      }

      mDelayedMessages = new DoubleQueue<DelayedMessage>();
      mHandler = new DefaultHandler();
      super.handle(message, envelop);
    }
  }

  private class StoppedHandler implements MessageHandler {

    public void handle(final Object message, @NotNull final Envelop envelop) {
      if (message == DUMMY_MESSAGE) {
        return;
      }

      mConversationNotifier.discard(message, envelop, mFailure);
    }
  }

  private class SuspendedHandler implements MessageHandler {

    public void handle(final Object message, @NotNull final Envelop envelop) {
      final ConversationNotifier conversationNotifier = mConversationNotifier;
      final Conversation<Object> failureConversation = mFailureConversation;
      final Actor sender = envelop.getSender();
      if (sender.equals(mSupervisor) && failureConversation.getThreadId()
          .equals(envelop.getThreadId())) {
        if (isFailureAborted(message)) {
          failureConversation.close();
          discardDelayed();
          stopSelf();
          return;

        } else if (isFailureRecovery(message)) {
          failureConversation.close();
          mHandler = new DefaultHandler();
          final RecoveryType recoveryType = ((SupervisedRecoveryMessage) message).getRecoveryType();
          if (recoveryType == RecoveryType.RESUME) {
            mHandler = new ResumedHandler();
            resumeDelayed();
            mMonitorNotifier.resume();

          } else if (recoveryType == RecoveryType.RESTART) {
            mHandler = new StoppedHandler();
            discardDelayed();
            restart();

          } else {
            mHandler = new StoppedHandler();
            discardDelayed();
            stopSelf();
          }

          return;
        }
      }

      if (message != null) {
        if (message == DUMMY_MESSAGE) {
          return;
        }

        final SignalingMessageHandler handler = mSuperviseHandlers.get(message.getClass());
        if (handler != null) {
          conversationNotifier.receive(message, envelop);
          handler.handle(message, envelop, DefaultContext.this);
          conversationNotifier.complete(message, envelop);
          return;
        }
      }

      mDelayedMessages.add(new DelayedMessage(message, envelop));
      conversationNotifier.delay(message, envelop);
    }
  }
}
