package dm.shakespeare2.template;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.UUID;

import dm.shakespeare.config.BuildConfig;
import dm.shakespeare.util.ConstantConditions;
import dm.shakespeare.util.DoubleQueue;
import dm.shakespeare2.actor.Actor;
import dm.shakespeare2.actor.ActorScript;
import dm.shakespeare2.actor.Behavior;
import dm.shakespeare2.actor.BehaviorBuilder.Handler;
import dm.shakespeare2.actor.Envelop;
import dm.shakespeare2.actor.Options;
import dm.shakespeare2.message.Bounce;
import dm.shakespeare2.message.DeadLetter;
import dm.shakespeare2.message.Failure;
import dm.shakespeare2.message.Receipt;
import dm.shakespeare2.template.SupervisedScript.SupervisedRecovery.RecoveryType;

/**
 * Created by davide-maestroni on 01/16/2019.
 */
public class SupervisedScript extends ActorScriptWrapper {

  private static final Supervise SUPERVISE = new Supervise();
  private static final Unsupervise UNSUPERVISE = new Unsupervise();

  public SupervisedScript(@NotNull final ActorScript script) {
    super(script);
  }

  @NotNull
  public static Supervise supervise() {
    return SUPERVISE;
  }

  @NotNull
  public static Unsupervise unsupervise() {
    return UNSUPERVISE;
  }

  @NotNull
  public Behavior getBehavior(@NotNull final String id) throws Exception {
    return new SupervisedBehavior(super.getBehavior(id));
  }

  public static class Supervise implements Serializable {

    private static final long serialVersionUID = BuildConfig.VERSION_HASH_CODE;

    private Supervise() {
    }
  }

  public static class SupervisedFailure implements Serializable {

    private static final long serialVersionUID = BuildConfig.VERSION_HASH_CODE;

    private final Throwable mCause;
    private final String mFailureId;

    private SupervisedFailure(@NotNull final String failureId, @NotNull final Throwable cause) {
      mFailureId = failureId;
      mCause = cause;
    }

    @NotNull
    public Throwable getCause() {
      return mCause;
    }

    @NotNull
    public final String getFailureId() {
      return mFailureId;
    }

    @NotNull
    public SupervisedRecovery recover(@NotNull final RecoveryType recoveryType) {
      return new SupervisedRecovery(mFailureId, recoveryType);
    }
  }

  public static class SupervisedRecovery implements Serializable {

    private static final long serialVersionUID = BuildConfig.VERSION_HASH_CODE;

    private final String mFailureId;
    private final RecoveryType mRecoveryType;

    private SupervisedRecovery(@NotNull final String failureId,
        @NotNull final RecoveryType recoveryType) {
      mFailureId = ConstantConditions.notNull("failureId", failureId);
      mRecoveryType = ConstantConditions.notNull("recoveryType", recoveryType);
    }

    @NotNull
    public final String getFailureId() {
      return mFailureId;
    }

    @NotNull
    public final RecoveryType getRecoveryType() {
      return mRecoveryType;
    }

    public enum RecoveryType {
      RETRY, RESUME, RESTART_AND_RETRY, RESTART_AND_RESUME, RESTART, DISMISS
    }
  }

  public static class Unsupervise implements Serializable {

    private static final long serialVersionUID = BuildConfig.VERSION_HASH_CODE;

    private Unsupervise() {
    }
  }

  private static class SupervisedBehavior implements Behavior {

    private static final Object DUMMY_MESSAGE = new Object();

    private final ContextWrapper mContext;
    private final String mReceiptId = toString();

    private Behavior mBehavior;
    private DoubleQueue<DelayedMessage> mDelayedMessages = new DoubleQueue<DelayedMessage>();
    private Throwable mFailure;
    private String mFailureId;
    private DelayedMessage mFailureMessage;
    private Handler<Object> mHandler = new DefaultHandler();
    private Actor mSupervisor;
    private String mSupervisorThread;

    private SupervisedBehavior(@NotNull final Behavior behavior) {
      mBehavior = ConstantConditions.notNull("behavior", behavior);
      mContext = new ContextWrapper() {

        @Override
        public void setBehavior(@NotNull final Behavior behavior) {
          mBehavior = ConstantConditions.notNull("behavior", behavior);
        }
      };
    }

    public void onMessage(final Object message, @NotNull final Envelop envelop,
        @NotNull final Context context) throws Exception {
      mHandler.handle(message, envelop, context);
    }

    public void onStart(@NotNull final Context context) throws Exception {
      mBehavior.onStart(mContext.withContext(context));
    }

    public void onStop(@NotNull final Context context) throws Exception {
      mBehavior.onStop(mContext.withContext(context));
    }

    private void discardDelayed(@NotNull final Actor self) {
      final Throwable failure = mFailure;
      final DelayedMessage failureMessage = mFailureMessage;
      if (failureMessage != null) {
        final Envelop envelop = failureMessage.getEnvelop();
        final Options options = envelop.getOptions();
        if (options.getReceiptId() != null) {
          envelop.getSender()
              .tell(new Failure(failureMessage.getMessage(), options, failure),
                  Options.thread(options.getThread()), self);
        }
      }

      final DoubleQueue<DelayedMessage> delayedMessages = mDelayedMessages;
      for (final DelayedMessage delayedMessage : delayedMessages) {
        final Envelop envelop = delayedMessage.getEnvelop();
        final Options options = envelop.getOptions();
        if (options.getReceiptId() != null) {
          envelop.getSender()
              .tell(new Failure(delayedMessage.getMessage(), options, failure),
                  Options.thread(options.getThread()), self);
        }
      }

      mDelayedMessages = new DoubleQueue<DelayedMessage>();
    }

    private void resetFailure(@NotNull final Actor self) {
      mSupervisor.removeObserver(self);
      mSupervisor = null;
      mSupervisorThread = null;
      mFailure = null;
      mFailureId = null;
      mFailureMessage = null;
    }

    private void resumeDelayed(@NotNull final Actor self) {
      final int size = mDelayedMessages.size();
      for (int i = 0; i < size; ++i) {
        self.tell(DUMMY_MESSAGE, null, self);
      }
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

    private class DefaultHandler implements Handler<Object> {

      public void handle(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) throws Exception {
        if (message == DUMMY_MESSAGE) {
          return;
        }

        final Options options = envelop.getOptions();
        if (message instanceof Supervise) {
          final Actor self = context.getSelf();
          final Actor sender = envelop.getSender();
          if (!sender.equals(self)) {
            mSupervisor = sender;
            mSupervisorThread = options.getThread();
            sender.addObserver(context.getSelf());

          } else if (options.getReceiptId() != null) {
            sender.tell(new Failure(message, options,
                    new IllegalArgumentException("an actor can't supervise itself")),
                Options.thread(options.getThread()), self);
            envelop.preventReceipt();
          }

        } else if (message instanceof Unsupervise) {
          final Actor sender = envelop.getSender();
          if (sender.equals(mSupervisor)) {
            mSupervisor.removeObserver(context.getSelf());
            mSupervisor = null;
            mSupervisorThread = null;

          } else if (options.getReceiptId() != null) {
            sender.tell(new Failure(message, options,
                    new IllegalArgumentException("sender is not the current supervisor")),
                Options.thread(options.getThread()), context.getSelf());
            envelop.preventReceipt();
          }

        } else if (message instanceof SupervisedRecovery) {
          final Actor sender = envelop.getSender();
          if (sender.equals(mSupervisor)) {
            context.getLogger().wrn("ignoring recovery message: " + message);

          } else if (options.getReceiptId() != null) {
            sender.tell(new Failure(message, options,
                    new IllegalArgumentException("sender is not the current supervisor")),
                Options.thread(options.getThread()), context.getSelf());
            envelop.preventReceipt();
          }

        } else if (Receipt.isReceipt(message, mReceiptId)) {
          context.getLogger().wrn("ignoring receipt message: " + message);

        } else if (message instanceof DeadLetter) {
          final Actor sender = envelop.getSender();
          if (sender.equals(mSupervisor)) {
            mSupervisor = null;
            mSupervisorThread = null;
          }

        } else {
          try {
            mBehavior.onMessage(message, envelop, mContext.withContext(context));

          } catch (final Throwable t) {
            if (!(t instanceof InterruptedException)) {
              final Actor supervisor = mSupervisor;
              if (supervisor != null) {
                final String failureId = UUID.randomUUID().toString();
                supervisor.tell(new SupervisedFailure(failureId, t),
                    new Options().withThread(mSupervisorThread).withReceiptId(mReceiptId),
                    context.getSelf());
                mFailure = t;
                mFailureId = failureId;
                mFailureMessage = new DelayedMessage(message, envelop);
                mHandler = new SuperviseHandler();
                envelop.preventReceipt();
                return;
              }
            }

            if (t instanceof RuntimeException) {
              throw (RuntimeException) t;

            } else if (t instanceof Exception) {
              throw (Exception) t;
            }
          }
        }
      }
    }

    private class ResumeHandler extends DefaultHandler {

      public void handle(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) throws Exception {
        final DoubleQueue<DelayedMessage> delayedMessages = mDelayedMessages;
        if (!delayedMessages.isEmpty()) {
          final DelayedMessage delayedMessage = delayedMessages.removeFirst();
          if (message != DUMMY_MESSAGE) {
            delayedMessages.add(new DelayedMessage(message, envelop));
          }

          super.handle(delayedMessage.getMessage(), delayedMessage.getEnvelop(), context);
          return;
        }

        mDelayedMessages = new DoubleQueue<DelayedMessage>();
        mHandler = new DefaultHandler();
        super.handle(message, envelop, context);
      }
    }

    private class SuperviseHandler implements Handler<Object> {

      public void handle(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) {
        if (message == DUMMY_MESSAGE) {
          return;
        }

        final Actor self = context.getSelf();
        final Options options = envelop.getOptions();
        if (message instanceof Supervise) {
          final Actor sender = envelop.getSender();
          if (!sender.equals(self)) {
            if (!sender.equals(mSupervisor)) {
              // TODO: 14/01/2019 notify old supervisor???
              mSupervisor = sender;
              mSupervisorThread = options.getThread();
              sender.addObserver(self);
              sender.tell(new SupervisedFailure(mFailureId, mFailure),
                  new Options().withThread(mSupervisorThread).withReceiptId(mReceiptId), self);
            }

          } else if (options.getReceiptId() != null) {
            sender.tell(new Failure(message, options,
                    new IllegalArgumentException("an actor can't supervise itself")),
                Options.thread(options.getThread()), self);
            envelop.preventReceipt();
          }

        } else if (message instanceof Unsupervise) {
          final Actor sender = envelop.getSender();
          if (sender.equals(mSupervisor)) {
            resetFailure(self);
            discardDelayed(self);
            context.dismissSelf();

          } else if (options.getReceiptId() != null) {
            sender.tell(new Failure(message, options,
                    new IllegalArgumentException("sender is not the current supervisor")),
                Options.thread(options.getThread()), self);
            envelop.preventReceipt();
          }

        } else if (message instanceof SupervisedRecovery) {
          final Actor sender = envelop.getSender();
          final SupervisedRecovery recovery = (SupervisedRecovery) message;
          if (sender.equals(mSupervisor)) {
            if (mFailureId.equals((recovery).getFailureId())) {
              final RecoveryType recoveryType = recovery.getRecoveryType();
              final DelayedMessage failureMessage = mFailureMessage;
              if (recoveryType == RecoveryType.RETRY) {
                mDelayedMessages.addFirst(failureMessage);
                resetFailure(self);
                mHandler = new ResumeHandler();
                resumeDelayed(self);

              } else if (recoveryType == RecoveryType.RESUME) {
                final Envelop failureEnvelop = failureMessage.getEnvelop();
                final Options failureOptions = failureEnvelop.getOptions();
                if (failureOptions.getReceiptId() != null) {
                  failureEnvelop.getSender()
                      .tell(new Failure(failureMessage.getMessage(), failureOptions, mFailure),
                          Options.thread(failureOptions.getThread()), self);
                }
                resetFailure(self);
                mHandler = new ResumeHandler();
                resumeDelayed(self);

              } else if (recoveryType == RecoveryType.RESTART_AND_RETRY) {
                mDelayedMessages.addFirst(failureMessage);
                resetFailure(self);
                mHandler = new ResumeHandler();
                resumeDelayed(self);
                context.restartSelf();

              } else if (recoveryType == RecoveryType.RESTART_AND_RESUME) {
                final Envelop failureEnvelop = failureMessage.getEnvelop();
                final Options failureOptions = failureEnvelop.getOptions();
                if (failureOptions.getReceiptId() != null) {
                  failureEnvelop.getSender()
                      .tell(new Failure(failureMessage.getMessage(), failureOptions, mFailure),
                          Options.thread(failureEnvelop.getOptions().getThread()), self);
                }
                resetFailure(self);
                mHandler = new ResumeHandler();
                resumeDelayed(self);
                context.restartSelf();

              } else if (recoveryType == RecoveryType.RESTART) {
                resetFailure(self);
                discardDelayed(self);
                context.restartSelf();

              } else {
                resetFailure(self);
                discardDelayed(self);
                context.dismissSelf();
              }

            } else if (options.getReceiptId() != null) {
              sender.tell(
                  new Failure(message, options, new IllegalArgumentException("invalid failure ID")),
                  Options.thread(options.getThread()), self);
              envelop.preventReceipt();
            }

          } else if (options.getReceiptId() != null) {
            sender.tell(new Failure(message, options,
                    new IllegalArgumentException("sender is not the current supervisor")),
                Options.thread(options.getThread()), self);
            envelop.preventReceipt();
          }

        } else if (Receipt.isReceipt(message, mReceiptId)) {
          if (message instanceof Bounce) {
            final Object bouncedMessage = ((Bounce) message).getMessage();
            if (bouncedMessage instanceof SupervisedFailure) {
              final Actor sender = envelop.getSender();
              if (sender.equals(mSupervisor) && mFailureId.equals(
                  ((SupervisedFailure) bouncedMessage).getFailureId())) {
                resetFailure(self);
                discardDelayed(self);
                context.dismissSelf();
              }
            }
          }

        } else if (message instanceof DeadLetter) {
          final Actor sender = envelop.getSender();
          if (sender.equals(mSupervisor)) {
            resetFailure(self);
            discardDelayed(self);
            context.dismissSelf();
          }

        } else {
          mDelayedMessages.add(new DelayedMessage(message, envelop));
          envelop.preventReceipt();
        }
      }
    }
  }
}
