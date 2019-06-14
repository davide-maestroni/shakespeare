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
import dm.shakespeare.actor.Headers;
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

  // TODO: 2019-06-14 enum + serializable(?) 

  public static final Supervise SUPERVISE = new Supervise();
  public static final Unsupervise UNSUPERVISE = new Unsupervise();

  private static final Object DUMMY_MESSAGE = new Object();

  private final AgentWrapper agent;
  private final String receiptId = toString();

  private Behavior behavior;
  private CQueue<DelayedMessage> delayedMessages = new CQueue<DelayedMessage>();
  private Throwable failure;
  private String failureId;
  private DelayedMessage failureMessage;
  private Handler<Object> handler = new DefaultHandler();
  private Actor supervisor;
  private String supervisorThread;

  SupervisedBehavior(@NotNull final Behavior behavior) {
    this.behavior = ConstantConditions.notNull("behavior", behavior);
    agent = new AgentWrapper() {

      @Override
      public void setBehavior(@NotNull final Behavior behavior) {
        SupervisedBehavior.this.behavior = ConstantConditions.notNull("behavior", behavior);
      }
    };
  }

  public void onMessage(final Object message, @NotNull final Envelop envelop,
      @NotNull final Agent agent) throws Exception {
    handler.handle(message, envelop, agent);
  }

  public void onStart(@NotNull final Agent agent) throws Exception {
    behavior.onStart(this.agent.withAgent(agent));
  }

  public void onStop(@NotNull final Agent agent) throws Exception {
    final Throwable failure = this.failure;
    final DelayedMessage failureMessage = this.failureMessage;
    if ((failure != null) && (failureMessage != null)) {
      final Envelop envelop = failureMessage.getEnvelop();
      final Headers headers = envelop.getHeaders();
      if (headers.getReceiptId() != null) {
        envelop.getSender()
            .tell(new Bounce(failureMessage.getMessage(), headers), headers.threadOnly(),
                agent.getSelf());
      }
    }
    resetFailure(agent);
    bounceDelayed(agent);
    behavior.onStop(this.agent.withAgent(agent));
  }

  private void bounceDelayed(@NotNull final Agent agent) {
    final CQueue<DelayedMessage> delayedMessages = this.delayedMessages;
    for (final DelayedMessage delayedMessage : delayedMessages) {
      final Envelop envelop = delayedMessage.getEnvelop();
      final Headers headers = envelop.getHeaders();
      if (headers.getReceiptId() != null) {
        envelop.getSender()
            .tell(new Bounce(delayedMessage.getMessage(), headers), headers.threadOnly(),
                agent.getSelf());
      }
    }
    this.delayedMessages = new CQueue<DelayedMessage>();
  }

  private void resetFailure(@NotNull final Agent agent) {
    try {
      supervisor.removeObserver(agent.getSelf());

    } catch (final RejectedExecutionException e) {
      agent.getLogger().err(e, "ignoring exception");
    }
    supervisor = null;
    supervisorThread = null;
    failure = null;
    failureId = null;
    failureMessage = null;
  }

  private void resumeDelayed(@NotNull final Agent agent) {
    final Actor self = agent.getSelf();
    final int size = delayedMessages.size();
    for (int i = 0; i < size; ++i) {
      self.tell(DUMMY_MESSAGE, null, self);
    }
  }

  public static class Supervise implements Serializable {

    private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

    private Supervise() {
    }
  }

  public static class SupervisedFailure implements Serializable {

    private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

    private final Throwable cause;
    private final String failureId;

    private SupervisedFailure(@NotNull final String failureId, @NotNull final Throwable cause) {
      this.failureId = failureId;
      this.cause = cause;
    }

    @NotNull
    public Throwable getCause() {
      return cause;
    }

    @NotNull
    public final String getFailureId() {
      return failureId;
    }

    @NotNull
    public SupervisedRecovery recover(@NotNull final RecoveryType recoveryType) {
      return new SupervisedRecovery(failureId, recoveryType);
    }
  }

  public static class SupervisedRecovery implements Serializable {

    private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

    private final String failureId;
    private final RecoveryType recoveryType;

    private SupervisedRecovery(@NotNull final String failureId,
        @NotNull final RecoveryType recoveryType) {
      this.failureId = ConstantConditions.notNull("failureId", failureId);
      this.recoveryType = ConstantConditions.notNull("recoveryType", recoveryType);
    }

    @NotNull
    public final String getFailureId() {
      return failureId;
    }

    @NotNull
    public final RecoveryType getRecoveryType() {
      return recoveryType;
    }

    public enum RecoveryType {
      RETRY, RESUME, RESTART_AND_RETRY, RESTART_AND_RESUME, RESTART, DISMISS
    }
  }

  public static class Unsupervise implements Serializable {

    private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

    private Unsupervise() {
    }
  }

  private static class DelayedMessage {

    private final Envelop envelop;
    private final Object message;

    private DelayedMessage(final Object message, @NotNull final Envelop envelop) {
      this.message = message;
      this.envelop = envelop;
    }

    @NotNull
    Envelop getEnvelop() {
      return envelop;
    }

    Object getMessage() {
      return message;
    }
  }

  private class DefaultHandler implements Handler<Object> {

    public void handle(final Object message, @NotNull final Envelop envelop,
        @NotNull final Agent agent) throws Exception {
      if (message == DUMMY_MESSAGE) {
        return;
      }
      final Headers headers = envelop.getHeaders();
      if (message instanceof Supervise) {
        final Actor self = agent.getSelf();
        final Actor sender = envelop.getSender();
        if (!sender.equals(self)) {
          supervisor = sender;
          supervisorThread = headers.getThreadId();
          sender.addObserver(agent.getSelf());

        } else if (headers.getReceiptId() != null) {
          sender.tell(new Failure(message, headers,
                  new IllegalRecipientException("an actor can't supervise itself")),
              headers.threadOnly(), self);
          envelop.preventReceipt();
        }

      } else if (message instanceof Unsupervise) {
        final Actor sender = envelop.getSender();
        if (sender.equals(supervisor)) {
          try {
            supervisor.removeObserver(agent.getSelf());

          } catch (final RejectedExecutionException e) {
            agent.getLogger().err(e, "ignoring exception");
          }
          supervisor = null;
          supervisorThread = null;

        } else if (headers.getReceiptId() != null) {
          sender.tell(new Failure(message, headers,
                  new IllegalStateException("sender is not the current supervisor")),
              headers.threadOnly(), agent.getSelf());
          envelop.preventReceipt();
        }

      } else if (message instanceof SupervisedRecovery) {
        final Actor sender = envelop.getSender();
        if (sender.equals(supervisor)) {
          agent.getLogger().wrn("ignoring recovery message: " + message);

        } else if (headers.getReceiptId() != null) {
          sender.tell(new Failure(message, headers,
                  new IllegalStateException("sender is not the current supervisor")),
              headers.threadOnly(), agent.getSelf());
          envelop.preventReceipt();
        }

      } else if (Receipt.isReceipt(message, receiptId)) {
        agent.getLogger().wrn("ignoring receipt message: " + message);

      } else if (message instanceof DeadLetter) {
        final Actor sender = envelop.getSender();
        if (sender.equals(supervisor)) {
          supervisor = null;
          supervisorThread = null;
        }

      } else {
        try {
          behavior.onMessage(message, envelop, SupervisedBehavior.this.agent.withAgent(agent));

        } catch (final Throwable t) {
          if (!(t instanceof InterruptedException)) {
            final Actor supervisor = SupervisedBehavior.this.supervisor;
            if (supervisor != null) {
              final String failureId = UUID.randomUUID().toString();
              supervisor.tell(new SupervisedFailure(failureId, t),
                  new Headers().withThreadId(supervisorThread).withReceiptId(receiptId),
                  agent.getSelf());
              failure = t;
              SupervisedBehavior.this.failureId = failureId;
              failureMessage = new DelayedMessage(message, envelop);
              handler = new SuperviseHandler();
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
        @NotNull final Agent agent) throws Exception {
      final CQueue<DelayedMessage> delayedMessages = SupervisedBehavior.this.delayedMessages;
      if (!delayedMessages.isEmpty()) {
        final DelayedMessage delayedMessage = delayedMessages.removeFirst();
        if (message != DUMMY_MESSAGE) {
          delayedMessages.add(new DelayedMessage(message, envelop));
        }
        super.handle(delayedMessage.getMessage(), delayedMessage.getEnvelop(), agent);
        return;
      }
      SupervisedBehavior.this.delayedMessages = new CQueue<DelayedMessage>();
      handler = new DefaultHandler();
      super.handle(message, envelop, agent);
    }
  }

  private class SuperviseHandler implements Handler<Object> {

    public void handle(final Object message, @NotNull final Envelop envelop,
        @NotNull final Agent agent) {
      if (message == DUMMY_MESSAGE) {
        return;
      }
      final Headers headers = envelop.getHeaders();
      if (message instanceof Supervise) {
        final Actor self = agent.getSelf();
        final Actor sender = envelop.getSender();
        if (!sender.equals(self)) {
          if (!sender.equals(supervisor)) {
            // TODO: 14/01/2019 notify old supervisor???
            supervisor = sender;
            supervisorThread = headers.getThreadId();
            sender.addObserver(self);
            sender.tell(new SupervisedFailure(failureId, failure),
                new Headers().withThreadId(supervisorThread).withReceiptId(receiptId), self);
          }

        } else if (headers.getReceiptId() != null) {
          sender.tell(new Failure(message, headers,
                  new IllegalRecipientException("an actor can't supervise itself")),
              headers.threadOnly(), self);
          envelop.preventReceipt();
        }

      } else if (message instanceof Unsupervise) {
        final Actor sender = envelop.getSender();
        if (sender.equals(supervisor)) {
          agent.dismissSelf();

        } else if (headers.getReceiptId() != null) {
          sender.tell(new Failure(message, headers,
                  new IllegalStateException("sender is not the current supervisor")),
              headers.threadOnly(), agent.getSelf());
          envelop.preventReceipt();
        }

      } else if (message instanceof SupervisedRecovery) {
        final Actor sender = envelop.getSender();
        final SupervisedRecovery recovery = (SupervisedRecovery) message;
        if (sender.equals(supervisor)) {
          if (failureId.equals((recovery).getFailureId())) {
            final RecoveryType recoveryType = recovery.getRecoveryType();
            final DelayedMessage failureMessage = SupervisedBehavior.this.failureMessage;
            if (recoveryType == RecoveryType.RETRY) {
              delayedMessages.addFirst(failureMessage);
              resetFailure(agent);
              handler = new ResumeHandler();
              resumeDelayed(agent);

            } else if (recoveryType == RecoveryType.RESUME) {
              final Envelop failureEnvelop = failureMessage.getEnvelop();
              final Headers failureHeaders = failureEnvelop.getHeaders();
              if (failureHeaders.getReceiptId() != null) {
                failureEnvelop.getSender()
                    .tell(new Failure(failureMessage.getMessage(), failureHeaders, failure),
                        failureHeaders.threadOnly(), agent.getSelf());
              }
              resetFailure(agent);
              handler = new ResumeHandler();
              resumeDelayed(agent);

            } else if (recoveryType == RecoveryType.RESTART_AND_RETRY) {
              delayedMessages.addFirst(failureMessage);
              resetFailure(agent);
              handler = new ResumeHandler();
              resumeDelayed(agent);
              agent.restartSelf();

            } else if (recoveryType == RecoveryType.RESTART_AND_RESUME) {
              final Envelop failureEnvelop = failureMessage.getEnvelop();
              final Headers failureHeaders = failureEnvelop.getHeaders();
              if (failureHeaders.getReceiptId() != null) {
                failureEnvelop.getSender()
                    .tell(new Failure(failureMessage.getMessage(), failureHeaders, failure),
                        failureHeaders.threadOnly(), agent.getSelf());
              }
              resetFailure(agent);
              handler = new ResumeHandler();
              resumeDelayed(agent);
              agent.restartSelf();

            } else if (recoveryType == RecoveryType.RESTART) {
              agent.restartSelf();

            } else {
              agent.dismissSelf();
            }

          } else if (headers.getReceiptId() != null) {
            sender.tell(
                new Failure(message, headers, new IllegalArgumentException("invalid failure ID")),
                headers.threadOnly(), agent.getSelf());
            envelop.preventReceipt();
          }

        } else if (headers.getReceiptId() != null) {
          sender.tell(new Failure(message, headers,
                  new IllegalStateException("sender is not the current supervisor")),
              headers.threadOnly(), agent.getSelf());
          envelop.preventReceipt();
        }

      } else if (Receipt.isReceipt(message, receiptId)) {
        if (message instanceof Bounce) {
          final Object bouncedMessage = ((Bounce) message).getMessage();
          if (bouncedMessage instanceof SupervisedFailure) {
            final Actor sender = envelop.getSender();
            if (sender.equals(supervisor) && failureId.equals(
                ((SupervisedFailure) bouncedMessage).getFailureId())) {
              agent.dismissSelf();
            }
          }
        }

      } else if (message instanceof DeadLetter) {
        final Actor sender = envelop.getSender();
        if (sender.equals(supervisor)) {
          agent.dismissSelf();
        }

      } else {
        delayedMessages.add(new DelayedMessage(message, envelop));
        envelop.preventReceipt();
      }
    }
  }
}
