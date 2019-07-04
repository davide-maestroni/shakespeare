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

import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.actor.Headers;
import dm.shakespeare.template.config.BuildConfig;

/**
 * Simple implementation of an {@code AbstractProxyBehavior} handling a single proxied actor.<br>
 * The proxied actor is set by sending a {@link AbstractProxyBehavior.ProxySignal#ADD_PROXIED
 * ProxySignal.ADD_PROXIED} message with the specific actor as sender. In the same way, it is unset
 * through a {@link AbstractProxyBehavior.ProxySignal#REMOVE_PROXIED ProxySignal.REMOVE_PROXIED}
 * message.<br>
 * If further messages of the above type are received, the proxied actor will change based on the
 * last message sender.<p>
 * When the behavior is serialized, the knowledge of the proxied actor will be lost.
 */
public class ProxyBehavior extends AbstractProxyBehavior {

  private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

  private transient Actor proxied;

  /**
   * {@inheritDoc}
   */
  @Override
  public void onMessage(final Object message, @NotNull final Envelop envelop,
      @NotNull final Agent agent) throws Exception {
    if (message == ProxySignal.ADD_PROXIED) {
      final Actor sender = envelop.getSender();
      agent.getLogger()
          .dbg("[%s] adding new proxied actor: envelop=%s - message=%s", agent.getSelf(), envelop,
              message);
      proxied = sender;

    } else if (message == ProxySignal.REMOVE_PROXIED) {
      final Actor sender = envelop.getSender();
      if (sender.equals(proxied)) {
        agent.getLogger()
            .dbg("[%s] removing proxied actor: envelop=%s - message=%s", agent.getSelf(), envelop,
                message);
        proxied = null;
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
    final Actor proxied = this.proxied;
    if (proxied != null) {
      agent.getLogger()
          .dbg("[%s] forwarding message to proxied actor: recipient=%s - sender=%s - headers=%s - "
              + "message=%s", agent.getSelf(), proxied, sender, headers, message);
      proxied.tell(message, headers.asSentAt(sentAt), sender);
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
