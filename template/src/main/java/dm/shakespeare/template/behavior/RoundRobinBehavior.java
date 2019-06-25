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
 * Created by davide-maestroni on 06/25/2019.
 */
public class RoundRobinBehavior extends ProxyBehavior {

  private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

  private transient final ArrayList<Actor> proxied = new ArrayList<Actor>();
  private transient final WeakHashMap<Actor, Actor> senders = new WeakHashMap<Actor, Actor>();

  private transient int current;

  @Override
  public void onMessage(final Object message, @NotNull final Envelop envelop,
      @NotNull final Agent agent) throws Exception {
    if (message == ProxySignal.ADD_PROXIED) {
      final Actor sender = envelop.getSender();
      final ArrayList<Actor> proxied = this.proxied;
      if (!proxied.contains(sender)) {
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
        proxied.remove(sender);
      }

    } else {
      super.onMessage(message, envelop, agent);
    }
  }

  protected void onIncoming(@NotNull final Actor sender, final Object message, final long sentAt,
      @NotNull final Headers headers, @NotNull final Agent agent) throws Exception {
    final ArrayList<Actor> proxied = this.proxied;
    if (proxied.isEmpty()) {
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
      actor.tell(message, headers.asSentAt(sentAt), agent.getSelf());
    }
  }
}
