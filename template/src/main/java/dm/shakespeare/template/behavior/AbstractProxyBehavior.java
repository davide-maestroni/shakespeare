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

import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;

import dm.shakespeare.Stage;
import dm.shakespeare.actor.AbstractBehavior;
import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.actor.Headers;
import dm.shakespeare.actor.Role;
import dm.shakespeare.actor.SerializableAbstractBehavior;
import dm.shakespeare.concurrent.ExecutorServices;
import dm.shakespeare.message.Bounce;
import dm.shakespeare.template.config.BuildConfig;
import dm.shakespeare.util.WeakValueHashMap;

/**
 * Abstract implementation of a behavior acting as proxy of one or more actors.
 */
public abstract class AbstractProxyBehavior extends SerializableAbstractBehavior {

  private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

  private transient final WeakValueHashMap<Actor, Actor> proxyToSenderMap =
      new WeakValueHashMap<Actor, Actor>();
  private transient final WeakHashMap<Actor, Actor> senderToProxyMap =
      new WeakHashMap<Actor, Actor>();

  /**
   * {@inheritDoc}
   */
  public void onMessage(final Object message, @NotNull final Envelop envelop,
      @NotNull final Agent agent) throws Exception {
    final WeakHashMap<Actor, Actor> senderToProxyMap = this.senderToProxyMap;
    final WeakValueHashMap<Actor, Actor> proxyToSenderMap = this.proxyToSenderMap;
    final Actor sender = envelop.getSender();
    final Headers headers = envelop.getHeaders();
    if (message instanceof OutgoingMessage) {
      final OutgoingMessage outgoingMessage = (OutgoingMessage) message;
      final Actor recipient = proxyToSenderMap.get(sender);
      if (recipient == null) {
        if (headers.getReceiptId() != null) {
          outgoingMessage.getSender()
              .tell(new Bounce(outgoingMessage.getMessage(), headers), headers.threadOnly(),
                  agent.getSelf());
        }

      } else {
        onOutgoing(outgoingMessage.getSender(), recipient, outgoingMessage.getMessage(),
            envelop.getSentAt(), headers, agent);
      }

    } else {
      final Actor self = agent.getSelf();
      if (!senderToProxyMap.containsKey(sender)) {
        proxyToSenderMap.keySet().retainAll(senderToProxyMap.values());
        final Actor proxy = Stage.newActor(sender.getId(), new SenderRole(self));
        senderToProxyMap.put(sender, proxy);
        proxyToSenderMap.put(proxy, sender);
      }
      final Actor proxy = senderToProxyMap.get(sender);
      if (proxy != null) {
        onIncoming(proxy, message, envelop.getSentAt(), envelop.getHeaders(), agent);
      }
    }
    envelop.preventReceipt();
  }

  /**
   * Handles an incoming messages.
   *
   * @param sender  the message sender.
   * @param message the message.
   * @param sentAt  the time at which the message has been sent.
   * @param headers the message headers.
   * @param agent   the behavior agent.
   * @throws Exception when an unexpected error occurs.
   */
  protected abstract void onIncoming(@NotNull Actor sender, Object message, long sentAt,
      @NotNull Headers headers, @NotNull Agent agent) throws Exception;

  /**
   * Handles an outgoing messages.
   *
   * @param sender    the message sender.
   * @param recipient the message recipient.
   * @param message   the message.
   * @param sentAt    the time at which the message has been sent.
   * @param headers   the message headers.
   * @param agent     the behavior agent.
   * @throws Exception when an unexpected error occurs.
   */
  protected abstract void onOutgoing(@NotNull Actor sender, @NotNull Actor recipient,
      Object message, long sentAt, @NotNull Headers headers, @NotNull Agent agent) throws Exception;

  private static class OutgoingMessage {

    private final Object message;
    private final Actor sender;

    private OutgoingMessage(final Object message, @NotNull final Actor sender) {
      this.message = message;
      this.sender = sender;
    }

    Object getMessage() {
      return message;
    }

    @NotNull
    Actor getSender() {
      return sender;
    }
  }

  private static class SenderRole extends Role {

    private final Actor proxy;

    private SenderRole(@NotNull final Actor proxy) {
      this.proxy = proxy;
    }

    @NotNull
    public Behavior getBehavior(@NotNull final String id) {
      return new AbstractBehavior() {

        public void onMessage(final Object message, @NotNull final Envelop envelop,
            @NotNull final Agent agent) {
          final Actor proxy = SenderRole.this.proxy;
          if (!proxy.equals(envelop.getSender())) {
            proxy.tell(new OutgoingMessage(message, envelop.getSender()),
                envelop.getHeaders().asSentAt(envelop.getSentAt()), agent.getSelf());
            envelop.preventReceipt();

          } else if (message instanceof Bounce) {
            final Bounce bounce = (Bounce) message;
            final Object originalMessage = (bounce).getMessage();
            if (originalMessage instanceof OutgoingMessage) {
              final Headers headers = bounce.getHeaders();
              final OutgoingMessage outgoingMessage = (OutgoingMessage) originalMessage;
              outgoingMessage.getSender()
                  .tell(new Bounce(outgoingMessage.getMessage(), headers), headers.threadOnly(),
                      agent.getSelf());
            }
          }
        }
      };
    }

    @NotNull
    @Override
    public ExecutorService getExecutorService(@NotNull final String id) {
      return ExecutorServices.localExecutor();
    }
  }
}
