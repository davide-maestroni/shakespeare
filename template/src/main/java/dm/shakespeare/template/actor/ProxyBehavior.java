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

import dm.shakespeare.BackStage;
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

  private final WeakReference<Actor> mActor;
  private final HashMap<Actor, WeakReference<Actor>> mProxyToSenderMap =
      new HashMap<Actor, WeakReference<Actor>>();
  private final WeakHashMap<Actor, Actor> mSenderToProxyMap = new WeakHashMap<Actor, Actor>();

  public ProxyBehavior(@NotNull final WeakReference<Actor> actorRef) {
    mActor = ConstantConditions.notNull("actorRef", actorRef);
  }

  public void onMessage(final Object message, @NotNull final Envelop envelop,
      @NotNull final Context context) throws Exception {
    final Actor actor = mActor.get();
    final WeakHashMap<Actor, Actor> senderToProxyMap = mSenderToProxyMap;
    final HashMap<Actor, WeakReference<Actor>> proxyToSenderMap = mProxyToSenderMap;
    final Actor sender = envelop.getSender();
    final Options options = envelop.getOptions();
    if (actor == null) {
      if (options.getReceiptId() != null) {
        sender.tell(new Bounce(message, options), options.threadOnly(), context.getSelf());
      }

      for (final Actor proxy : senderToProxyMap.values()) {
        proxy.dismiss(false);
      }
      context.dismissSelf();

    } else if (actor.equals(sender)) {
      if (options.getReceiptId() != null) {
        sender.tell(new Failure(message, options,
                new IllegalRecipientException("an actor can't proxy itself")), options.threadOnly(),
            context.getSelf());
      }

    } else if (proxyToSenderMap.containsKey(sender)) {
      final Actor recipient = proxyToSenderMap.get(sender).get();
      if (recipient == null) {
        if (options.getReceiptId() != null) {
          sender.tell(new Bounce(message, options), options.threadOnly(), context.getSelf());

        } else {
          sender.dismiss(false);
        }

      } else {
        onOutgoing(actor, recipient, message, envelop.getSentAt(), options, context);
      }

    } else {
      final Actor self = context.getSelf();
      if (!senderToProxyMap.containsKey(sender)) {
        proxyToSenderMap.keySet().retainAll(senderToProxyMap.values());
        final Actor proxy = BackStage.newActor(sender.getId(), new SenderRole(self, actor));
        senderToProxyMap.put(sender, proxy);
        proxyToSenderMap.put(proxy, new WeakReference<Actor>(sender));
      }
      onIncoming(actor, sender, message, envelop.getSentAt(), envelop.getOptions(), context);
    }
    envelop.preventReceipt();
  }

  protected void onIncoming(@NotNull final Actor proxied, @NotNull final Actor sender,
      final Object message, final long sentAt, @NotNull final Options options,
      @NotNull final Context context) throws Exception {
    final Actor proxy = mSenderToProxyMap.get(sender);
    if (proxy != null) {
      proxied.tell(message, options.asSentAt(sentAt), proxy);
    }
  }

  protected void onOutgoing(@NotNull final Actor proxied, @NotNull final Actor recipient,
      final Object message, final long sentAt, @NotNull final Options options,
      @NotNull final Context context) throws Exception {
    recipient.tell(message, options.asSentAt(sentAt), context.getSelf());
  }

  private static class SenderRole extends Role {

    private final Actor mProxied;
    private final Actor mProxy;

    private SenderRole(@NotNull final Actor proxy, @NotNull final Actor proxied) {
      mProxy = proxy;
      mProxied = proxied;
    }

    @NotNull
    public Behavior getBehavior(@NotNull final String id) {
      return new AbstractBehavior() {

        public void onMessage(final Object message, @NotNull final Envelop envelop,
            @NotNull final Context context) {
          final Actor proxy = mProxy;
          if (proxy.equals(envelop.getSender())) {
            mProxied.tell(message, envelop.getOptions().asSentAt(envelop.getSentAt()),
                context.getSelf());
            context.dismissSelf();

          } else {
            proxy.tell(message, envelop.getOptions().asSentAt(envelop.getSentAt()),
                context.getSelf());
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
