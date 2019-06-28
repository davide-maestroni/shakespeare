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

package dm.shakespeare.template.behavior;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.UUID;

import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.BehaviorBuilder.Handler;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.actor.Headers;
import dm.shakespeare.actor.SerializableAbstractBehavior;
import dm.shakespeare.message.Bounce;
import dm.shakespeare.message.DeadLetter;
import dm.shakespeare.message.Failure;
import dm.shakespeare.message.Receipt;
import dm.shakespeare.template.behavior.SupervisedBehavior.SupervisedRecovery.RecoveryType;
import dm.shakespeare.template.config.BuildConfig;
import dm.shakespeare.util.CQueue;
import dm.shakespeare.util.ConstantConditions;

/**
 * Class wrapping a {@code Behavior} instance so add supervision functionalities.<br>
 * A supervised behavior can be restored from a failure (that is, an exception is thrown during
 * the processing of a message), when a supervisor actor is set. If a failure occurs, the supervisor
 * will be notified with a {@link SupervisedFailure} message, and a {@link SupervisedRecovery} is
 * expected as reply. The type of recovery may range from retry to dismissal of the supervised
 * actor (see {@link RecoveryType}). Messages received during after the failure are handled
 * accordingly, so that they might be automatically resent to the supervised actor or bounced.<p>
 * It is possible to set (or reset) a supervisor by sending a {@link SupervisedSignal#SUPERVISE}
 * message to the supervised actor, with the supervisor as sender. When a supervisor is replaced
 * by a new one, the former will receive a {@link SupervisedSignal#REPLACE_SUPERVISOR} by the
 * supervised actor.<br>
 * In case a supervisor is unset or is dismissed while the supervised actor is in failure state, the
 * supervised actor will be automatically dismissed.
 * <p>
 * When the behavior is serialized, the knowledge of the supervisor actor and the failure state will
 * be lost.
 */
public class SupervisedBehavior extends SerializableAbstractBehavior {

  private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

  private transient final String receiptId = toString();

  private transient SupervisedAgentWrapper agent;
  private Behavior behavior;
  private transient CQueue<DelayedMessage> delayedMessages = new CQueue<DelayedMessage>();
  private transient Throwable failure;
  private transient String failureId;
  private transient DelayedMessage failureMessage;
  private transient Handler<Object> handler = new DefaultHandler();
  private transient Actor supervisor;
  private transient String supervisorThread;

  /**
   * Creates a new supervised behavior wrapping the specified instance.
   *
   * @param behavior the wrapped behavior.
   */
  public SupervisedBehavior(@NotNull final Behavior behavior) {
    this.behavior = ConstantConditions.notNull("behavior", behavior);
  }

  /**
   * Creates an empty behavior.<br>
   * Usually needed during deserialization.
   */
  SupervisedBehavior() {
    behavior = null;
  }

  /**
   * Returns the wrapped behavior.<br>
   * Usually needed during serialization.
   *
   * @return th behavior instance.
   */
  @NotNull
  public Behavior getBehavior() {
    return behavior;
  }

  /**
   * {@inheritDoc}
   */
  public void onMessage(final Object message, @NotNull final Envelop envelop,
      @NotNull final Agent agent) throws Exception {
    handler.handle(message, envelop, wrap(agent));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onStart(@NotNull final Agent agent) throws Exception {
    behavior.onStart(wrap(agent));
  }

  /**
   * {@inheritDoc}
   */
  @Override
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
    behavior.onStop(wrap(agent));
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
    supervisor.removeObserver(agent.getSelf());
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
      self.tell(SupervisedDelayedSignal.DUMMY_MESSAGE, Headers.EMPTY, self);
    }
  }

  @NotNull
  private Agent wrap(@NotNull final Agent agent) {
    if (this.agent == null) {
      this.agent = new SupervisedAgentWrapper(agent);
    }
    return this.agent;
  }

  /**
   * Supervised signalling messages.
   */
  public enum SupervisedSignal {

    /**
     * Notifies that the sender of the message wants to become the new supervisor.
     */
    SUPERVISE,

    /**
     * Notifies that the sender of the message wants to be removed as supervisor.
     */
    UNSUPERVISE,

    /**
     * Notifies the recipient of the message that it has been replaced as supervisor of the sender.
     */
    REPLACE_SUPERVISOR

  }

  private enum SupervisedDelayedSignal {
    DUMMY_MESSAGE
  }

  /**
   * Message notifying the supervisor that the supervised actor has entered a failure state.
   */
  public static class SupervisedFailure implements Serializable {

    private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

    private final Throwable cause;
    private final String failureId;

    /**
     * Creates an empty message.<br>
     * Usually needed during deserialization.
     */
    private SupervisedFailure() {
      failureId = "";
      cause = new UnsupportedOperationException();
    }

    /**
     * Creates a new supervised failure message.
     *
     * @param failureId the failure ID.
     * @param cause     the cause of the failure.
     */
    private SupervisedFailure(@NotNull final String failureId, @NotNull final Throwable cause) {
      this.failureId = failureId;
      this.cause = cause;
    }

    /**
     * Returns the cause of the failure.
     *
     * @return the throwable instance.
     */
    @NotNull
    public Throwable getCause() {
      return cause;
    }

    /**
     * Returns the unique failure ID.
     *
     * @return the failure ID.
     */
    @NotNull
    public final String getFailureId() {
      return failureId;
    }

    /**
     * Creates a new recovery message to be sent as reply of this one.
     *
     * @param recoveryType the type of recovery.
     * @return the supervised recovery message.
     */
    @NotNull
    public SupervisedRecovery recover(@NotNull final RecoveryType recoveryType) {
      return new SupervisedRecovery(failureId, recoveryType);
    }
  }

  /**
   * Message indicating to the supervised actor how to recover itself from the failure state.
   */
  public static class SupervisedRecovery implements Serializable {

    private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

    private final String failureId;
    private final RecoveryType recoveryType;

    /**
     * Creates an empty message.<br>
     * Usually needed during deserialization.
     */
    private SupervisedRecovery() {
      failureId = "";
      recoveryType = RecoveryType.DISMISS;
    }

    /**
     * Creates a new supervised recovery message.
     *
     * @param failureId    the failure ID.
     * @param recoveryType the type of recovery.
     */
    private SupervisedRecovery(@NotNull final String failureId,
        @NotNull final RecoveryType recoveryType) {
      this.failureId = ConstantConditions.notNull("failureId", failureId);
      this.recoveryType = ConstantConditions.notNull("recoveryType", recoveryType);
    }

    /**
     * Returns the unique failure ID.
     *
     * @return the failure ID.
     */
    @NotNull
    public final String getFailureId() {
      return failureId;
    }

    /**
     * Returns the type of recovery to be applied.
     *
     * @return the recovery type.
     */
    @NotNull
    public final RecoveryType getRecoveryType() {
      return recoveryType;
    }

    /**
     * Enumeration indicating the possible types of recovery from the failure state.
     */
    public enum RecoveryType {

      /**
       * Tells the supervised actor to retry the processing of the message that caused the failure.
       * <br>
       * In case of success, all the pending messages will be processed in arrival order.
       */
      RETRY,

      /**
       * Tells the supervised actor to drop the message that caused the failure and go on processing
       * the pending messages still in the inbox.
       */
      RESUME,

      /**
       * Tells the supervised actor to restart its behavior and retry the processing of the message
       * that caused the failure.<br>
       * In case of success, all the pending messages will be processed in arrival order.
       */
      RESTART_AND_RETRY,

      /**
       * Tells the supervised actor to restart its behavior and go on processing the pending
       * messages still in the inbox, while dropping the message that caused the failure.
       */
      RESTART_AND_RESUME,

      /**
       * Tells the supervised actor to restart its behavior and drop the message that caused the
       * failure along with all the pending messages still in the inbox.
       */
      RESTART,

      /**
       * Tells the supervised actor to dismiss itself and drop the message that caused the failure
       * along with all the pending messages still in the inbox.
       */
      DISMISS

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
      if (message == SupervisedDelayedSignal.DUMMY_MESSAGE) {
        return;
      }
      final Actor self = agent.getSelf();
      final Headers headers = envelop.getHeaders();
      if (message == SupervisedSignal.SUPERVISE) {
        final Actor sender = envelop.getSender();
        if (!sender.equals(self)) {
          if (sender.addObserver(agent.getSelf())) {
            agent.getLogger()
                .dbg("[%s] supervisor successfully set: envelop=%s - message=%s", self, envelop,
                    message);
            supervisor = sender;
            supervisorThread = headers.getThreadId();

          } else {
            agent.getLogger()
                .err("[%s] failed to add observer to supervisor: envelop=%s - message=%s", self,
                    envelop, message);
            if (headers.getReceiptId() != null) {
              sender.tell(new Failure(message, headers,
                      new IllegalRecipientException("can't add observer to supervisor")),
                  headers.threadOnly(), self);
              envelop.preventReceipt();
            }
          }

        } else {
          agent.getLogger()
              .err("[%s] cannot set self as supervisor: envelop=%s - message=%s", self, envelop,
                  message);
          if (headers.getReceiptId() != null) {
            sender.tell(new Failure(message, headers,
                    new IllegalRecipientException("an actor can't supervise itself")),
                headers.threadOnly(), self);
            envelop.preventReceipt();
          }
        }

      } else if (message == SupervisedSignal.UNSUPERVISE) {
        final Actor sender = envelop.getSender();
        if (sender.equals(supervisor)) {
          agent.getLogger()
              .dbg("[%s] supervisor successfully unset: envelop=%s - message=%s", self, envelop,
                  message);
          supervisor.removeObserver(self);
          supervisor = null;
          supervisorThread = null;

        } else {
          agent.getLogger()
              .err("[%s] cannot unset supervisor: envelop=%s - message=%s", self, envelop, message);
          if (headers.getReceiptId() != null) {
            sender.tell(new Failure(message, headers,
                    new IllegalStateException("sender is not the current supervisor")),
                headers.threadOnly(), self);
            envelop.preventReceipt();
          }
        }

      } else if (message instanceof SupervisedRecovery) {
        final Actor sender = envelop.getSender();
        if (sender.equals(supervisor)) {
          agent.getLogger()
              .wrn("[%s] ignoring recovery message, not in failure state: envelop=%s - message=%s",
                  self, envelop, message);

        } else {
          if (headers.getReceiptId() != null) {
            agent.getLogger()
                .err("[%s] ignoring recovery message: envelop=%s - message=%s", self, envelop,
                    message);
            sender.tell(new Failure(message, headers,
                    new IllegalStateException("sender is not the current supervisor")),
                headers.threadOnly(), agent.getSelf());
            envelop.preventReceipt();
          }
        }

      } else if (Receipt.isReceipt(message, receiptId)) {
        agent.getLogger()
            .wrn("[%s] ignoring receipt message, not in failure state: envelop=%s - message=%s",
                self, envelop, message);

      } else if (message instanceof DeadLetter) {
        final Actor sender = envelop.getSender();
        if (sender.equals(supervisor)) {
          agent.getLogger()
              .dbg("[%s] supervisor successfully unset: envelop=%s - message=%s", self, envelop,
                  message);
          supervisor = null;
          supervisorThread = null;
        }

      } else {
        try {
          behavior.onMessage(message, envelop, SupervisedBehavior.this.agent);

        } catch (final Throwable t) {
          agent.getLogger()
              .err(t, "[%s] message processing failure: envelop=%s - message=%s", self, envelop,
                  message);
          if (t instanceof Error) {
            // rethrow errors
            throw (Error) t;
          }

          if (!(t instanceof InterruptedException)) {
            final Actor supervisor = SupervisedBehavior.this.supervisor;
            if (supervisor != null) {
              final String failureId = UUID.randomUUID().toString();
              agent.getLogger()
                  .dbg("[%s] sending failure to supervisor: failureId=%s - supervisor=%s", self,
                      failureId, supervisor);
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
          throw (Exception) t;
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
        if (message != SupervisedDelayedSignal.DUMMY_MESSAGE) {
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
      if (message == SupervisedDelayedSignal.DUMMY_MESSAGE) {
        return;
      }
      final Actor self = agent.getSelf();
      final Headers headers = envelop.getHeaders();
      if (message == SupervisedSignal.SUPERVISE) {
        final Actor sender = envelop.getSender();
        if (!sender.equals(self)) {
          if (!sender.equals(supervisor)) {
            if (sender.addObserver(self)) {
              agent.getLogger()
                  .dbg("[%s] supervisor successfully replaced: supervisor=%s - envelop=%s - "
                      + "message=%s", self, supervisor, envelop, message);
              // notify old supervisor
              supervisor.tell(SupervisedSignal.REPLACE_SUPERVISOR, Headers.EMPTY, self);
              supervisor = sender;
              supervisorThread = headers.getThreadId();
              sender.tell(new SupervisedFailure(failureId, failure),
                  new Headers().withThreadId(supervisorThread).withReceiptId(receiptId), self);

            } else {
              agent.getLogger()
                  .err("[%s] failed to add observer to supervisor: envelop=%s - message=%s", self,
                      envelop, message);
              if (headers.getReceiptId() != null) {
                sender.tell(new Failure(message, headers,
                        new IllegalRecipientException("can't add observer to supervisor")),
                    headers.threadOnly(), self);
                envelop.preventReceipt();
              }
            }
          }

        } else {
          agent.getLogger()
              .err("[%s] cannot set self as supervisor: envelop=%s - message=%s", self, envelop,
                  message);
          if (headers.getReceiptId() != null) {
            sender.tell(new Failure(message, headers,
                    new IllegalRecipientException("an actor can't supervise itself")),
                headers.threadOnly(), self);
            envelop.preventReceipt();
          }
        }

      } else if (message == SupervisedSignal.UNSUPERVISE) {
        final Actor sender = envelop.getSender();
        if (sender.equals(supervisor)) {
          agent.getLogger()
              .dbg("[%s] supervisor successfully unset: envelop=%s - message=%s", self, envelop,
                  message);
          self.dismiss();

        } else {
          if (headers.getReceiptId() != null) {
            sender.tell(new Failure(message, headers,
                    new IllegalStateException("sender is not the current supervisor")),
                headers.threadOnly(), self);
            envelop.preventReceipt();
          }
        }

      } else if (message instanceof SupervisedRecovery) {
        final Actor sender = envelop.getSender();
        final SupervisedRecovery recovery = (SupervisedRecovery) message;
        if (sender.equals(supervisor)) {
          if (failureId.equals((recovery).getFailureId())) {
            agent.getLogger()
                .dbg("[%s] handling supervised recovery: envelop=%s - message=%s", self, envelop,
                    message);
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
                        failureHeaders.threadOnly(), self);
              }
              resetFailure(agent);
              handler = new ResumeHandler();
              resumeDelayed(agent);

            } else if (recoveryType == RecoveryType.RESTART_AND_RETRY) {
              delayedMessages.addFirst(failureMessage);
              resetFailure(agent);
              handler = new ResumeHandler();
              resumeDelayed(agent);
              agent.restartBehavior();

            } else if (recoveryType == RecoveryType.RESTART_AND_RESUME) {
              final Envelop failureEnvelop = failureMessage.getEnvelop();
              final Headers failureHeaders = failureEnvelop.getHeaders();
              if (failureHeaders.getReceiptId() != null) {
                failureEnvelop.getSender()
                    .tell(new Failure(failureMessage.getMessage(), failureHeaders, failure),
                        failureHeaders.threadOnly(), self);
              }
              resetFailure(agent);
              handler = new ResumeHandler();
              resumeDelayed(agent);
              agent.restartBehavior();

            } else if (recoveryType == RecoveryType.RESTART) {
              agent.restartBehavior();

            } else {
              self.dismiss();
            }

          } else {
            agent.getLogger()
                .dbg("[%s] ignoring supervised recovery, invalid failure ID: envelop=%s - "
                    + "message=%s", self, envelop, message);
            if (headers.getReceiptId() != null) {
              sender.tell(
                  new Failure(message, headers, new IllegalArgumentException("invalid failure ID")),
                  headers.threadOnly(), self);
              envelop.preventReceipt();
            }
          }

        } else {
          agent.getLogger()
              .dbg("[%s] ignoring supervised recovery, invalid supervisor: envelop=%s - "
                  + "message=%s", self, envelop, message);
          if (headers.getReceiptId() != null) {
            sender.tell(new Failure(message, headers,
                    new IllegalStateException("sender is not the current supervisor")),
                headers.threadOnly(), self);
            envelop.preventReceipt();
          }
        }

      } else if (Receipt.isReceipt(message, receiptId)) {
        if (message instanceof Bounce) {
          final Object bouncedMessage = ((Bounce) message).getMessage();
          if (bouncedMessage instanceof SupervisedFailure) {
            final Actor sender = envelop.getSender();
            if (sender.equals(supervisor) && failureId.equals(
                ((SupervisedFailure) bouncedMessage).getFailureId())) {
              agent.getLogger()
                  .wrn("[%s] failure bounce received: envelop=%s - message=%s", self, envelop,
                      message);
              self.dismiss();
            }
          }
        }

      } else if (message instanceof DeadLetter) {
        final Actor sender = envelop.getSender();
        if (sender.equals(supervisor)) {
          agent.getLogger()
              .dbg("[%s] supervisor successfully unset: envelop=%s - message=%s", self, envelop,
                  message);
          self.dismiss();
        }

      } else {
        agent.getLogger()
            .dbg("[%s] delaying message: envelop=%s - message=%s", self, envelop, message);
        delayedMessages.add(new DelayedMessage(message, envelop));
        envelop.preventReceipt();
      }
    }
  }

  private class SupervisedAgentWrapper extends AgentWrapper {

    private SupervisedAgentWrapper(@NotNull final Agent agent) {
      super(agent);
    }

    @Override
    public void setBehavior(@NotNull final Behavior behavior) {
      SupervisedBehavior.this.behavior = ConstantConditions.notNull("behavior", behavior);
    }
  }
}
