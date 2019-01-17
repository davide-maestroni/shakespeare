package dm.shakespeare2.template;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;

import dm.shakespeare.executor.ExecutorServices;
import dm.shakespeare.util.ConstantConditions;
import dm.shakespeare2.LocalStage;
import dm.shakespeare2.actor.AbstractBehavior;
import dm.shakespeare2.actor.Actor;
import dm.shakespeare2.actor.ActorScript;
import dm.shakespeare2.actor.Behavior;
import dm.shakespeare2.actor.Behavior.Context;
import dm.shakespeare2.actor.Envelop;
import dm.shakespeare2.actor.Options;
import dm.shakespeare2.message.Bounce;
import dm.shakespeare2.message.Failure;
import dm.shakespeare2.message.IllegalRecipientException;

/**
 * Created by davide-maestroni on 01/16/2019.
 */
public class ProxyScript extends ActorScript {

  private final WeakReference<Actor> mActor;
  private final HashMap<Actor, WeakReference<Actor>> mProxyToSenderMap =
      new HashMap<Actor, WeakReference<Actor>>();
  private final WeakHashMap<Actor, Actor> mSenderToProxyMap = new WeakHashMap<Actor, Actor>();

  public ProxyScript(@NotNull final Actor actor) {
    mActor = new WeakReference<Actor>(ConstantConditions.notNull("actor", actor));
  }

  @NotNull
  @Override
  public Behavior getBehavior(@NotNull final String id) {
    return new AbstractBehavior() {

      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) throws Exception {
        final Actor actor = mActor.get();
        final WeakHashMap<Actor, Actor> senderToProxyMap = mSenderToProxyMap;
        final HashMap<Actor, WeakReference<Actor>> proxyToSenderMap = mProxyToSenderMap;
        final Actor sender = envelop.getSender();
        final Options options = envelop.getOptions();
        if (actor == null) {
          if (options.getReceiptId() != null) {
            envelop.getSender()
                .tell(new Bounce(message, options), Options.thread(options.getThread()),
                    context.getSelf());
          }

          for (final Actor proxy : senderToProxyMap.values()) {
            proxy.dismiss(false);
          }
          context.dismissSelf();

        } else if (actor.equals(sender)) {
          if (options.getReceiptId() != null) {
            sender.tell(new Failure(message, options,
                    new IllegalRecipientException("an actor can't proxy itself")),
                Options.thread(options.getThread()), context.getSelf());
          }

        } else if (proxyToSenderMap.containsKey(sender)) {
          final Actor recipient = proxyToSenderMap.get(sender).get();
          if (recipient == null) {
            if (options.getReceiptId() != null) {
              sender.tell(new Bounce(message, options), Options.thread(options.getThread()),
                  context.getSelf());

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
            final Actor proxy =
                new LocalStage().newActor(sender.getId(), new SenderScript(self, actor));
            senderToProxyMap.put(sender, proxy);
            proxyToSenderMap.put(proxy, new WeakReference<Actor>(sender));
          }
          onIncoming(actor, sender, message, envelop.getSentAt(), envelop.getOptions(), context);
        }
        envelop.preventReceipt();
      }
    };
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

  private static class SenderScript extends ActorScript {

    private final Actor mProxied;
    private final Actor mProxy;

    private SenderScript(@NotNull final Actor proxy, @NotNull final Actor proxied) {
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
    public ExecutorService getExecutor(@NotNull final String id) {
      return ExecutorServices.trampolineExecutor();
    }
  }
}
