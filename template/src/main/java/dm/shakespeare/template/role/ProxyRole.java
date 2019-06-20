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

package dm.shakespeare.template.role;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;

import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Behavior.Agent;
import dm.shakespeare.actor.Headers;
import dm.shakespeare.actor.Role;
import dm.shakespeare.template.behavior.ProxyBehavior;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 03/24/2019.
 */
public class ProxyRole extends Role {

  private final WeakReference<Actor> actorRef;

  public ProxyRole(@NotNull final Actor actor) {
    actorRef = new WeakReference<Actor>(ConstantConditions.notNull("actor", actor));
  }

  @NotNull
  public Behavior getBehavior(@NotNull final String id) {
    return new ProxyBehavior(actorRef) {

      @Override
      protected void onIncoming(@NotNull final Actor proxied, @NotNull final Actor sender,
          final Object message, final long sentAt, @NotNull final Headers headers,
          @NotNull final Agent agent) throws Exception {
        if (ProxyRole.this.onIncoming(proxied, sender, message, sentAt, headers, agent)) {
          super.onIncoming(proxied, sender, message, sentAt, headers, agent);
        }
      }

      @Override
      protected void onOutgoing(@NotNull final Actor proxied, @NotNull final Actor recipient,
          final Object message, final long sentAt, @NotNull final Headers headers,
          @NotNull final Agent agent) throws Exception {
        if (ProxyRole.this.onOutgoing(proxied, recipient, message, sentAt, headers, agent)) {
          super.onOutgoing(proxied, recipient, message, sentAt, headers, agent);
        }
      }
    };
  }

  protected boolean onIncoming(@NotNull final Actor proxied, @NotNull final Actor sender,
      final Object message, final long sentAt, @NotNull final Headers headers,
      @NotNull final Agent agent) throws Exception {
    return true;
  }

  protected boolean onOutgoing(@NotNull final Actor proxied, @NotNull final Actor recipient,
      final Object message, final long sentAt, @NotNull final Headers headers,
      @NotNull final Agent agent) throws Exception {
    return true;
  }
}
