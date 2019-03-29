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

package dm.shakespeare.template.actor;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;

import dm.shakespeare.actor.AbstractBehavior;
import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.BehaviorBuilder.Handler;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.actor.Options;
import dm.shakespeare.message.Bounce;
import dm.shakespeare.message.DeadLetter;
import dm.shakespeare.message.Failure;
import dm.shakespeare.message.Receipt;
import dm.shakespeare.template.actor.SupervisedBehavior.SupervisedRecovery.RecoveryType;
import dm.shakespeare.template.config.BuildConfig;
import dm.shakespeare.util.CQueue;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 03/24/2019.
 */
public class SupervisedBehavior extends AbstractBehavior {

  public static final Supervise SUPERVISE = new Supervise();
  public static final Unsupervise UNSUPERVISE = new Unsupervise();

  private static final Object DUMMY_MESSAGE = new Object();

  private final ContextWrapper mContext;
  private final String mReceiptId = toString();

  private Behavior mBehavior;
  private CQueue<DelayedMessage> mDelayedMessages = new CQueue<DelayedMessage>();
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

  public void onMessage(final Object message, @NotNull final Envelop envelop,
      @NotNull final Context context) throws Exception {
    mHandler.handle(message, envelop, context);
  }

  public void onStart(@NotNull final Context context) throws Exception {
    mBehavior.onStart(mContext.withContext(context));
  }

  public void onStop(@NotNull final Context context) throws Exception {
    final Throwable failure = mFailure;
    final DelayedMessage failureMessage = mFailureMessage;
    if ((failure != null) && (failureMessage != null)) {
      final Envelop envelop = failureMessage.getEnvelop();
      final Options options = envelop.getOptions();
      if (options.getReceiptId() != null) {
        envelop.getSender()
            .tell(new Bounce(failureMessage.getMessage(), options), options.threadOnly(),
                context.getSelf());
      }
    }
    resetFailure(context);
    bounceDelayed(context);
    mBehavior.onStop(mContext.withContext(context));
  }

  private void bounceDelayed(@NotNull final Context context) {
    final CQueue<DelayedMessage> delayedMessages = mDelayedMessages;
    for (final DelayedMessage delayedMessage : delayedMessages) {
      final Envelop envelop = delayedMessage.getEnvelop();
      final Options options = envelop.getOptions();
      if (options.getReceiptId() != null) {
        envelop.getSender()
            .tell(new Bounce(delayedMessage.getMessage(), options), options.threadOnly(),
                context.getSelf());
      }
    }
    mDelayedMessages = new CQueue<DelayedMessage>();
  }

  private void resetFailure(@NotNull final Context context) {
    try {
      mSupervisor.removeObserver(context.getSelf());

    } catch (final RejectedExecutionException e) {
      context.getLogger().err(e, "ignoring exception");
    }
    mSupervisor = null;
    mSupervisorThread = null;
    mFailure = null;
    mFailureId = null;
    mFailureMessage = null;
  }

  private void resumeDelayed(@NotNull final Context context) {
    final Actor self = context.getSelf();
    final int size = mDelayedMessages.size();
    for (int i = 0; i < size; ++i) {
      self.tell(DUMMY_MESSAGE, null, self);
    }
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
          mSupervisorThread = options.getThreadId();
          sender.addObserver(context.getSelf());

        } else if (options.getReceiptId() != null) {
          sender.tell(new Failure(message, options,
                  new IllegalRecipientException("an actor can't supervise itself")),
              options.threadOnly(), self);
          envelop.preventReceipt();
        }

      } else if (message instanceof Unsupervise) {
        final Actor sender = envelop.getSender();
        if (sender.equals(mSupervisor)) {
          try {
            mSupervisor.removeObserver(context.getSelf());

          } catch (final RejectedExecutionException e) {
            context.getLogger().err(e, "ignoring exception");
          }
          mSupervisor = null;
          mSupervisorThread = null;

        } else if (options.getReceiptId() != null) {
          sender.tell(new Failure(message, options,
                  new IllegalStateException("sender is not the current supervisor")),
              options.threadOnly(), context.getSelf());
          envelop.preventReceipt();
        }

      } else if (message instanceof SupervisedRecovery) {
        final Actor sender = envelop.getSender();
        if (sender.equals(mSupervisor)) {
          context.getLogger().wrn("ignoring recovery message: " + message);

        } else if (options.getReceiptId() != null) {
          sender.tell(new Failure(message, options,
                  new IllegalStateException("sender is not the current supervisor")),
              options.threadOnly(), context.getSelf());
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
                  new Options().withThreadId(mSupervisorThread).withReceiptId(mReceiptId),
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
      final CQueue<DelayedMessage> delayedMessages = mDelayedMessages;
      if (!delayedMessages.isEmpty()) {
        final DelayedMessage delayedMessage = delayedMessages.removeFirst();
        if (message != DUMMY_MESSAGE) {
          delayedMessages.add(new DelayedMessage(message, envelop));
        }
        super.handle(delayedMessage.getMessage(), delayedMessage.getEnvelop(), context);
        return;
      }
      mDelayedMessages = new CQueue<DelayedMessage>();
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
            mSupervisorThread = options.getThreadId();
            sender.addObserver(self);
            sender.tell(new SupervisedFailure(mFailureId, mFailure),
                new Options().withThreadId(mSupervisorThread).withReceiptId(mReceiptId), self);
          }

        } else if (options.getReceiptId() != null) {
          sender.tell(new Failure(message, options,
                  new IllegalRecipientException("an actor can't supervise itself")),
              options.threadOnly(), self);
          envelop.preventReceipt();
        }

      } else if (message instanceof Unsupervise) {
        final Actor sender = envelop.getSender();
        if (sender.equals(mSupervisor)) {
          context.dismissSelf();

        } else if (options.getReceiptId() != null) {
          sender.tell(new Failure(message, options,
                  new IllegalStateException("sender is not the current supervisor")),
              options.threadOnly(), context.getSelf());
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
              resetFailure(context);
              mHandler = new ResumeHandler();
              resumeDelayed(context);

            } else if (recoveryType == RecoveryType.RESUME) {
              final Envelop failureEnvelop = failureMessage.getEnvelop();
              final Options failureOptions = failureEnvelop.getOptions();
              if (failureOptions.getReceiptId() != null) {
                failureEnvelop.getSender()
                    .tell(new Failure(failureMessage.getMessage(), failureOptions, mFailure),
                        failureOptions.threadOnly(), context.getSelf());
              }
              resetFailure(context);
              mHandler = new ResumeHandler();
              resumeDelayed(context);

            } else if (recoveryType == RecoveryType.RESTART_AND_RETRY) {
              mDelayedMessages.addFirst(failureMessage);
              resetFailure(context);
              mHandler = new ResumeHandler();
              resumeDelayed(context);
              context.restartSelf();

            } else if (recoveryType == RecoveryType.RESTART_AND_RESUME) {
              final Envelop failureEnvelop = failureMessage.getEnvelop();
              final Options failureOptions = failureEnvelop.getOptions();
              if (failureOptions.getReceiptId() != null) {
                failureEnvelop.getSender()
                    .tell(new Failure(failureMessage.getMessage(), failureOptions, mFailure),
                        failureOptions.threadOnly(), context.getSelf());
              }
              resetFailure(context);
              mHandler = new ResumeHandler();
              resumeDelayed(context);
              context.restartSelf();

            } else if (recoveryType == RecoveryType.RESTART) {
              context.restartSelf();

            } else {
              context.dismissSelf();
            }

          } else if (options.getReceiptId() != null) {
            sender.tell(
                new Failure(message, options, new IllegalArgumentException("invalid failure ID")),
                options.threadOnly(), context.getSelf());
            envelop.preventReceipt();
          }

        } else if (options.getReceiptId() != null) {
          sender.tell(new Failure(message, options,
                  new IllegalStateException("sender is not the current supervisor")),
              options.threadOnly(), context.getSelf());
          envelop.preventReceipt();
        }

      } else if (Receipt.isReceipt(message, mReceiptId)) {
        if (message instanceof Bounce) {
          final Object bouncedMessage = ((Bounce) message).getMessage();
          if (bouncedMessage instanceof SupervisedFailure) {
            final Actor sender = envelop.getSender();
            if (sender.equals(mSupervisor) && mFailureId.equals(
                ((SupervisedFailure) bouncedMessage).getFailureId())) {
              context.dismissSelf();
            }
          }
        }

      } else if (message instanceof DeadLetter) {
        final Actor sender = envelop.getSender();
        if (sender.equals(mSupervisor)) {
          context.dismissSelf();
        }

      } else {
        mDelayedMessages.add(new DelayedMessage(message, envelop));
        envelop.preventReceipt();
      }
    }
  }
}
