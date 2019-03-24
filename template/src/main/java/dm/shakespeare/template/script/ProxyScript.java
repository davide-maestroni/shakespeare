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

package dm.shakespeare.template.script;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;

import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Behavior.Context;
import dm.shakespeare.actor.Options;
import dm.shakespeare.actor.Script;
import dm.shakespeare.template.actor.ProxyBehavior;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 03/24/2019.
 */
public class ProxyScript extends Script {

  private final WeakReference<Actor> mActorRef;

  public ProxyScript(@NotNull final Actor actor) {
    mActorRef = new WeakReference<Actor>(ConstantConditions.notNull("actor", actor));
  }

  @NotNull
  public Behavior getBehavior(@NotNull final String id) {
    return new ProxyBehavior(mActorRef) {

      @Override
      protected void onIncoming(@NotNull final Actor proxied, @NotNull final Actor sender,
          final Object message, final long sentAt, @NotNull final Options options,
          @NotNull final Context context) throws Exception {
        if (ProxyScript.this.onIncoming(proxied, sender, message, sentAt, options, context)) {
          super.onIncoming(proxied, sender, message, sentAt, options, context);
        }
      }

      @Override
      protected void onOutgoing(@NotNull final Actor proxied, @NotNull final Actor recipient,
          final Object message, final long sentAt, @NotNull final Options options,
          @NotNull final Context context) throws Exception {
        if (ProxyScript.this.onOutgoing(proxied, recipient, message, sentAt, options, context)) {
          super.onOutgoing(proxied, recipient, message, sentAt, options, context);
        }
      }
    };
  }

  protected boolean onIncoming(@NotNull final Actor proxied, @NotNull final Actor sender,
      final Object message, final long sentAt, @NotNull final Options options,
      @NotNull final Context context) throws Exception {
    return true;
  }

  protected boolean onOutgoing(@NotNull final Actor proxied, @NotNull final Actor recipient,
      final Object message, final long sentAt, @NotNull final Options options,
      @NotNull final Context context) throws Exception {
    return true;
  }
}
