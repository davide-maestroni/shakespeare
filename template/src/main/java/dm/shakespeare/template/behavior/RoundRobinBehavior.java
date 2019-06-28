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

import java.util.ArrayList;
import java.util.WeakHashMap;

import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.actor.Headers;
import dm.shakespeare.message.Bounce;
import dm.shakespeare.template.config.BuildConfig;

/**
 * {@code Behavior} implementing a load balancer of other actors.<br>
 * New balanced actors are added by sending a {@link ProxySignal#ADD_PROXIED} message with the
 * proxied actor as sender. In the same way, balanced actors are removed through a
 * {@link ProxySignal#REMOVE_PROXIED} message.<p>
 * Each actor, communicating with the balancer, will be assigned a recipient based on a round-robin
 * algorithm. Such recipient will not change for further messages coming from the same actor.
 * Notice, however, that different recipients might be assigned to different sender actor.<p>
 * When the behavior is serialized, the knowledge of the proxied actors will be lost.
 */
public class RoundRobinBehavior extends AbstractProxyBehavior {

  private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

  private transient final ArrayList<Actor> proxied = new ArrayList<Actor>();
  private transient final WeakHashMap<Actor, Actor> senders = new WeakHashMap<Actor, Actor>();

  private transient int current;

  /**
   * {@inheritDoc}
   */
  @Override
  public void onMessage(final Object message, @NotNull final Envelop envelop,
      @NotNull final Agent agent) throws Exception {
    if (message == ProxySignal.ADD_PROXIED) {
      final Actor sender = envelop.getSender();
      final ArrayList<Actor> proxied = this.proxied;
      if (!proxied.contains(sender)) {
        agent.getLogger()
            .dbg("[%s] adding new proxied actor: envelop=%s - message=%s", agent.getSelf(), envelop,
                message);
        proxied.add(sender);
      }

    } else if (message == ProxySignal.REMOVE_PROXIED) {
      final Actor sender = envelop.getSender();
      final ArrayList<Actor> proxied = this.proxied;
      final int index = proxied.indexOf(sender);
      if (index >= 0) {
        if (index < current) {
          --current;
        }
        agent.getLogger()
            .dbg("[%s] removing proxied actor: envelop=%s - message=%s", agent.getSelf(), envelop,
                message);
        proxied.remove(sender);
      }

    } else {
      super.onMessage(message, envelop, agent);
    }
  }

  /**
   * {@inheritDoc}
   */
  protected void onIncoming(@NotNull final Actor sender, final Object message, final long sentAt,
      @NotNull final Headers headers, @NotNull final Agent agent) throws Exception {
    final ArrayList<Actor> proxied = this.proxied;
    if (proxied.isEmpty()) {
      agent.getLogger()
          .wrn("[%s] no proxied actor present, bouncing message: sender=%s - headers=%s - "
              + "message=%s", agent.getSelf(), sender, headers, message);
      sender.tell(new Bounce(message, headers), headers.threadOnly(), agent.getSelf());

    } else {
      final WeakHashMap<Actor, Actor> senders = this.senders;
      Actor actor = senders.get(sender);
      if (actor == null) {
        final int size = proxied.size();
        actor = proxied.get(current % size);
        if (++current >= size) {
          current = 0;
        }
        senders.put(sender, actor);
      }
      agent.getLogger()
          .dbg("[%s] forwarding message to proxied actor: recipient=%s - sender=%s - headers=%s - "
              + "message=%s", agent.getSelf(), actor, sender, headers, message);
      actor.tell(message, headers.asSentAt(sentAt), agent.getSelf());
    }
  }

  /**
   * {@inheritDoc}
   */
  protected void onOutgoing(@NotNull final Actor sender, @NotNull final Actor recipient,
      final Object message, final long sentAt, @NotNull final Headers headers,
      @NotNull final Agent agent) throws Exception {
    agent.getLogger()
        .dbg("[%s] forwarding message from proxied actor: recipient=%s - sender=%s - headers=%s - "
            + "message=%s", agent.getSelf(), recipient, sender, headers, message);
    recipient.tell(message, headers.asSentAt(sentAt), agent.getSelf());
  }
}
