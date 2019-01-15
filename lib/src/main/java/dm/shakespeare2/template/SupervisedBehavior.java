package dm.shakespeare2.template;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import dm.shakespeare.actor.SignalingMessage;
import dm.shakespeare.util.ConstantConditions;
import dm.shakespeare.util.DoubleQueue;
import dm.shakespeare2.actor.Actor;
import dm.shakespeare2.actor.Behavior;
import dm.shakespeare2.actor.BehaviorBuilder.Handler;
import dm.shakespeare2.actor.Envelop;
import dm.shakespeare2.actor.Options;
import dm.shakespeare2.message.Bounce;
import dm.shakespeare2.message.Failure;
import dm.shakespeare2.template.SupervisedBehavior.SupervisedRecovery.RecoveryType;

/**
 * Created by davide-maestroni on 01/14/2019.
 */
public class SupervisedBehavior implements Behavior {

  private static final SignalingMessage DUMMY_MESSAGE = new SignalingMessage() {};
  private static final Supervise SUPERVISE = new Supervise();
  private static final Unsupervise UNSUPERVISE = new Unsupervise();

  private final ContextWrapper mContext;

  private Behavior mBehavior;
  private DoubleQueue<DelayedMessage> mDelayedMessages = new DoubleQueue<DelayedMessage>();
  private Throwable mFailure;
  private String mFailureId;
  private DelayedMessage mFailureMessage;
  private Handler<Object> mHandler = new DefaultHandler();
  private Actor mSupervisor;
  private String mSupervisorThread;

  SupervisedBehavior(@NotNull final Behavior behavior) {
    mBehavior = ConstantConditions.notNull("behavior", behavior);
    mContext = new ContextWrapper() {

      @Override
      public void setBehavior(@NotNull final Behavior behavior) {
        mBehavior = ConstantConditions.notNull("behavior", behavior);
      }
    };
  }

  @NotNull
  private static Supervise supervise() {
    return SUPERVISE;
  }

  @NotNull
  private static Unsupervise unsupervise() {
    return UNSUPERVISE;
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

  private void discardDelayed(@NotNull final Context context) {
    final Throwable failure = mFailure;
    final DelayedMessage failureMessage = mFailureMessage;
    if (failureMessage != null) {
      final Envelop envelop = failureMessage.getEnvelop();
      final Options options = envelop.getOptions();
      if (options.getFailure()) {
        envelop.getSender()
            .tell(new Failure(failureMessage.getMessage(), options, failure),
                Options.thread(options.getThread()), context.getSelf());
      }
    }

    final DoubleQueue<DelayedMessage> delayedMessages = mDelayedMessages;
    for (final DelayedMessage delayedMessage : delayedMessages) {
      final Envelop envelop = delayedMessage.getEnvelop();
      final Options options = envelop.getOptions();
      if (options.getFailure()) {
        envelop.getSender()
            .tell(new Failure(delayedMessage.getMessage(), options, failure),
                Options.thread(options.getThread()), context.getSelf());
      }
    }

    mDelayedMessages = new DoubleQueue<DelayedMessage>();
  }

  private void resetFailure() {
    mSupervisor = null;
    mSupervisorThread = null;
    mFailure = null;
    mFailureId = null;
    mFailureMessage = null;
  }

  private void resumeDelayed(@NotNull final Context context) {
    final Actor actor = context.getSelf();
    final int size = mDelayedMessages.size();
    for (int i = 0; i < size; ++i) {
      actor.tell(DUMMY_MESSAGE, null, actor);
    }
  }

  public static class Supervise {

    private Supervise() {
    }
  }

  public static class SupervisedFailure {

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

  public static class SupervisedRecovery {

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

  public static class Unsupervise {

    private Unsupervise() {
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

        } else if (options.getFailure()) {
          sender.tell(new Failure(message, options,
                  new IllegalArgumentException("an actor can't supervise itself")),
              Options.thread(options.getThread()), self);
        }
        return;

      } else if (message instanceof Unsupervise) {
        final Actor sender = envelop.getSender();
        if (sender.equals(mSupervisor)) {
          mSupervisor = null;
          mSupervisorThread = null;

        } else if (options.getFailure()) {
          sender.tell(new Failure(message, options,
                  new IllegalArgumentException("sender is not the current supervisor")),
              Options.thread(options.getThread()), context.getSelf());
        }
        return;
      }

      try {
        mBehavior.onMessage(message, envelop, mContext.withContext(context));

      } catch (final Throwable t) {
        if (!(t instanceof InterruptedException)) {
          final Actor supervisor = mSupervisor;
          if (supervisor != null) {
            final String failureId = UUID.randomUUID().toString();
            supervisor.tell(new SupervisedFailure(failureId, t),
                new Options().withThread(mSupervisorThread).withBounce(true).withFailure(true),
                context.getSelf());
            mFailure = t;
            mFailureId = failureId;
            mFailureMessage = new DelayedMessage(message, envelop);
            mHandler = new SuperviseHandler();
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

      final Options options = envelop.getOptions();
      if (message instanceof Supervise) {
        final Actor self = context.getSelf();
        final Actor sender = envelop.getSender();
        if (!sender.equals(self)) {
          if (!sender.equals(mSupervisor)) {
            // TODO: 14/01/2019 notify old supervisor???
            mSupervisor = sender;
            mSupervisorThread = options.getThread();
            sender.tell(new SupervisedFailure(mFailureId, mFailure),
                new Options().withThread(mSupervisorThread).withBounce(true).withFailure(true),
                context.getSelf());
          }

        } else if (options.getFailure()) {
          sender.tell(new Failure(message, options,
                  new IllegalArgumentException("an actor can't supervise itself")),
              Options.thread(options.getThread()), self);
        }
        return;

      } else if (message instanceof Unsupervise) {
        final Actor sender = envelop.getSender();
        if (sender.equals(mSupervisor)) {
          resetFailure();
          discardDelayed(context);
          context.dismissSelf();

        } else if (options.getFailure()) {
          sender.tell(new Failure(message, options,
                  new IllegalArgumentException("sender is not the current supervisor")),
              Options.thread(options.getThread()), context.getSelf());
        }
        return;

      } else if (message instanceof SupervisedRecovery) {
        final Actor sender = envelop.getSender();
        final SupervisedRecovery recovery = (SupervisedRecovery) message;
        if (sender.equals(mSupervisor)) {
          if (mFailureId.equals((recovery).getFailureId())) {
            final RecoveryType recoveryType = recovery.getRecoveryType();
            final DelayedMessage failureMessage = mFailureMessage;
            if (recoveryType == RecoveryType.RETRY) {
              mDelayedMessages.addFirst(failureMessage);
              resetFailure();
              mHandler = new ResumeHandler();
              resumeDelayed(context);

            } else if (recoveryType == RecoveryType.RESUME) {
              final Envelop failureEnvelop = failureMessage.getEnvelop();
              final Options failureOptions = failureEnvelop.getOptions();
              if (failureOptions.getFailure()) {
                failureEnvelop.getSender()
                    .tell(new Failure(failureMessage.getMessage(), failureOptions, mFailure),
                        Options.thread(failureOptions.getThread()), context.getSelf());
              }
              resetFailure();
              mHandler = new ResumeHandler();
              resumeDelayed(context);

            } else if (recoveryType == RecoveryType.RESTART_AND_RETRY) {
              mDelayedMessages.addFirst(failureMessage);
              resetFailure();
              mHandler = new ResumeHandler();
              resumeDelayed(context);
              context.restartSelf();

            } else if (recoveryType == RecoveryType.RESTART_AND_RESUME) {
              final Envelop failureEnvelop = failureMessage.getEnvelop();
              final Options failureOptions = failureEnvelop.getOptions();
              if (failureOptions.getFailure()) {
                failureEnvelop.getSender()
                    .tell(new Failure(failureMessage.getMessage(), failureOptions, mFailure),
                        Options.thread(failureEnvelop.getOptions().getThread()), context.getSelf());
              }
              resetFailure();
              mHandler = new ResumeHandler();
              resumeDelayed(context);
              context.restartSelf();

            } else if (recoveryType == RecoveryType.RESTART) {
              resetFailure();
              discardDelayed(context);
              context.restartSelf();

            } else {
              resetFailure();
              discardDelayed(context);
              context.dismissSelf();
            }

          } else if (options.getFailure()) {
            sender.tell(
                new Failure(message, options, new IllegalArgumentException("invalid failure ID")),
                Options.thread(options.getThread()), context.getSelf());
          }

        } else if (options.getFailure()) {
          sender.tell(new Failure(message, options,
                  new IllegalArgumentException("sender is not the current supervisor")),
              Options.thread(options.getThread()), context.getSelf());
        }
        return;

      } else if (message instanceof Bounce) {
        final Object bouncedMessage = ((Bounce) message).getMessage();
        if (bouncedMessage instanceof SupervisedFailure) {
          final Actor sender = envelop.getSender();
          if (sender.equals(mSupervisor) && mFailureId.equals(
              ((SupervisedFailure) bouncedMessage).getFailureId())) {
            resetFailure();
            discardDelayed(context);
            context.dismissSelf();
            return;
          }
        }
      }

      mDelayedMessages.add(new DelayedMessage(message, envelop));
    }
  }
}
