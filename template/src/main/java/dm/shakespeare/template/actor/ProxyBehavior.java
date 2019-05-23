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

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;

import dm.shakespeare.Stage;
import dm.shakespeare.actor.AbstractBehavior;
import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.actor.Options;
import dm.shakespeare.actor.Role;
import dm.shakespeare.concurrent.ExecutorServices;
import dm.shakespeare.message.Bounce;
import dm.shakespeare.message.Failure;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 03/24/2019.
 */
public class ProxyBehavior extends AbstractBehavior {

  private final WeakReference<Actor> actor;
  private final HashMap<Actor, WeakReference<Actor>> proxyToSenderMap =
      new HashMap<Actor, WeakReference<Actor>>();
  private final WeakHashMap<Actor, Actor> senderToProxyMap = new WeakHashMap<Actor, Actor>();

  public ProxyBehavior(@NotNull final WeakReference<Actor> actorRef) {
    actor = ConstantConditions.notNull("actorRef", actorRef);
  }

  public void onMessage(final Object message, @NotNull final Envelop envelop,
      @NotNull final Agent agent) throws Exception {
    final Actor actor = this.actor.get();
    final WeakHashMap<Actor, Actor> senderToProxyMap = this.senderToProxyMap;
    final HashMap<Actor, WeakReference<Actor>> proxyToSenderMap = this.proxyToSenderMap;
    final Actor sender = envelop.getSender();
    final Options options = envelop.getOptions();
    if (actor == null) {
      if (options.getReceiptId() != null) {
        sender.tell(new Bounce(message, options), options.threadOnly(), agent.getSelf());
      }

      for (final Actor proxy : senderToProxyMap.values()) {
        proxy.dismiss(false);
      }
      agent.dismissSelf();

    } else if (actor.equals(sender)) {
      if (options.getReceiptId() != null) {
        sender.tell(new Failure(message, options,
                new IllegalRecipientException("an actor can't proxy itself")), options.threadOnly(),
            agent.getSelf());
      }

    } else if (proxyToSenderMap.containsKey(sender)) {
      final Actor recipient = proxyToSenderMap.get(sender).get();
      if (recipient == null) {
        if (options.getReceiptId() != null) {
          sender.tell(new Bounce(message, options), options.threadOnly(), agent.getSelf());

        } else {
          sender.dismiss(false);
        }

      } else {
        onOutgoing(actor, recipient, message, envelop.getSentAt(), options, agent);
      }

    } else {
      final Actor self = agent.getSelf();
      if (!senderToProxyMap.containsKey(sender)) {
        proxyToSenderMap.keySet().retainAll(senderToProxyMap.values());
        final Actor proxy = Stage.newActor(sender.getId(), new SenderRole(self, actor));
        senderToProxyMap.put(sender, proxy);
        proxyToSenderMap.put(proxy, new WeakReference<Actor>(sender));
      }
      onIncoming(actor, sender, message, envelop.getSentAt(), envelop.getOptions(), agent);
    }
    envelop.preventReceipt();
  }

  protected void onIncoming(@NotNull final Actor proxied, @NotNull final Actor sender,
      final Object message, final long sentAt, @NotNull final Options options,
      @NotNull final Agent agent) throws Exception {
    final Actor proxy = senderToProxyMap.get(sender);
    if (proxy != null) {
      proxied.tell(message, options.asSentAt(sentAt), proxy);
    }
  }

  protected void onOutgoing(@NotNull final Actor proxied, @NotNull final Actor recipient,
      final Object message, final long sentAt, @NotNull final Options options,
      @NotNull final Agent agent) throws Exception {
    recipient.tell(message, options.asSentAt(sentAt), agent.getSelf());
  }

  private static class SenderRole extends Role {

    private final Actor proxied;
    private final Actor proxy;

    private SenderRole(@NotNull final Actor proxy, @NotNull final Actor proxied) {
      this.proxy = proxy;
      this.proxied = proxied;
    }

    @NotNull
    public Behavior getBehavior(@NotNull final String id) {
      return new AbstractBehavior() {

        public void onMessage(final Object message, @NotNull final Envelop envelop,
            @NotNull final Agent agent) {
          final Actor proxy = SenderRole.this.proxy;
          if (proxy.equals(envelop.getSender())) {
            proxied.tell(message, envelop.getOptions().asSentAt(envelop.getSentAt()),
                agent.getSelf());
            agent.dismissSelf();

          } else {
            proxy.tell(message, envelop.getOptions().asSentAt(envelop.getSentAt()),
                agent.getSelf());
            envelop.preventReceipt();
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
