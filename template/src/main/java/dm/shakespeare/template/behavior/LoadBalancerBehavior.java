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

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.WeakHashMap;

import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.actor.Headers;
import dm.shakespeare.message.Bounce;
import dm.shakespeare.message.Receipt;
import dm.shakespeare.template.config.BuildConfig;

/**
 * {@code Behavior} implementing a load balancer of other actors.<br>
 * New balanced actors are added by sending a {@link ProxySignal#ADD_PROXIED} message with the
 * proxied actor as sender. In the same way, balanced actors are removed through a
 * {@link ProxySignal#REMOVE_PROXIED} message.<p>
 * Each actor, communicating with the balancer, will be assigned a recipient based on the minimum
 * inbox size at the time the first message is received. Such recipient will not change for further
 * messages coming from the same actor. Notice, however, that different recipients might be assigned
 * to different sender actor.<p>
 * When the behavior is serialized, the knowledge of the proxied actors will be lost.
 */
public class LoadBalancerBehavior extends AbstractProxyBehavior {

  private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

  private transient final HashMap<Actor, Integer> proxied = new HashMap<Actor, Integer>();
  private transient final String receiptId = toString();
  private transient final WeakHashMap<Actor, Actor> senders = new WeakHashMap<Actor, Actor>();

  /**
   * {@inheritDoc}
   */
  @Override
  public void onMessage(final Object message, @NotNull final Envelop envelop,
      @NotNull final Agent agent) throws Exception {
    if (message == ProxySignal.ADD_PROXIED) {
      final Actor sender = envelop.getSender();
      final HashMap<Actor, Integer> proxied = this.proxied;
      if (!proxied.containsKey(sender)) {
        proxied.put(sender, 0);
      }

    } else if (message == ProxySignal.REMOVE_PROXIED) {
      proxied.remove(envelop.getSender());

    } else {
      super.onMessage(message, envelop, agent);
    }
  }

  /**
   * {@inheritDoc}
   */
  protected void onIncoming(@NotNull final Actor sender, final Object message, final long sentAt,
      @NotNull final Headers headers, @NotNull final Agent agent) throws Exception {
    final HashMap<Actor, Integer> proxied = this.proxied;
    if (proxied.isEmpty()) {
      sender.tell(new Bounce(message, headers), headers.threadOnly(), agent.getSelf());

    } else {
      final WeakHashMap<Actor, Actor> senders = this.senders;
      Actor actor = senders.get(sender);
      if (actor == null) {
        Integer min = null;
        for (final Entry<Actor, Integer> entry : proxied.entrySet()) {
          final Integer count = entry.getValue();
          if ((min == null) || (count < min)) {
            min = count;
            actor = entry.getKey();
          }
        }
      }
      if (actor != null) {
        proxied.put(actor, proxied.get(actor) + 1);
        actor.tell(message, decorateHeaders(headers.asSentAt(sentAt)), agent.getSelf());
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void onOutgoing(@NotNull final Actor sender, @NotNull final Actor recipient,
      final Object message, final long sentAt, @NotNull Headers headers,
      @NotNull final Agent agent) throws Exception {
    if (message instanceof Receipt) {
      final HashMap<Actor, Integer> proxied = this.proxied;
      final Integer count = proxied.get(sender);
      if (count != null) {
        proxied.put(sender, Math.max(0, count - 1));
      }
      headers = resetHeaders((Receipt) message, headers);
    }
    recipient.tell(message, headers.asSentAt(sentAt), agent.getSelf());
  }

  @NotNull
  private Headers decorateHeaders(@NotNull final Headers headers) {
    if (headers.getReceiptId() == null) {
      return headers.withReceiptId(receiptId);
    }
    return headers;
  }

  @NotNull
  private Headers resetHeaders(@NotNull final Receipt receipt, @NotNull final Headers headers) {
    if (this.receiptId.equals(receipt.getHeaders().getReceiptId())) {
      return headers.withReceiptId(null);
    }
    return headers;
  }
}
