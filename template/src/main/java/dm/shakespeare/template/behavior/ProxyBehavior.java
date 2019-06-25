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
 * Created by davide-maestroni on 06/25/2019.
 */
public class ProxyBehavior extends AbstractProxyBehavior {

  private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

  private transient Actor proxied;

  @Override
  public void onMessage(final Object message, @NotNull final Envelop envelop,
      @NotNull final Agent agent) throws Exception {
    if (message == ProxySignal.ADD_PROXIED) {
      proxied = envelop.getSender();

    } else if (message == ProxySignal.REMOVE_PROXIED) {
      proxied = null;

    } else {
      super.onMessage(message, envelop, agent);
    }
  }

  protected void onIncoming(@NotNull final Actor sender, final Object message, final long sentAt,
      @NotNull final Headers headers, @NotNull final Agent agent) throws Exception {
    if (proxied != null) {
      proxied.tell(message, headers.asSentAt(sentAt), agent.getSelf());
    }
  }

  protected void onOutgoing(@NotNull final Actor sender, @NotNull final Actor recipient,
      final Object message, final long sentAt, @NotNull final Headers headers,
      @NotNull final Agent agent) throws Exception {
    recipient.tell(message, headers.asSentAt(sentAt), agent.getSelf());
  }

  public enum ProxySignal {
    ADD_PROXIED, REMOVE_PROXIED
  }
}
