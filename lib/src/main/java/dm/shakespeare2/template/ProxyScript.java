package dm.shakespeare2.template;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;

import dm.shakespeare.executor.ExecutorServices;
import dm.shakespeare.util.ConstantConditions;
import dm.shakespeare2.LocalStage;
import dm.shakespeare2.actor.Actor;
import dm.shakespeare2.actor.ActorScript;
import dm.shakespeare2.actor.Behavior;
import dm.shakespeare2.actor.Behavior.Context;
import dm.shakespeare2.actor.Envelop;
import dm.shakespeare2.actor.Options;
import dm.shakespeare2.message.DeadLetter;

/**
 * Created by davide-maestroni on 01/16/2019.
 */
public class ProxyScript extends ActorScript {

  private final Actor mActor;
  private final HashMap<Actor, WeakReference<Actor>> mProxyToSenderMap =
      new HashMap<Actor, WeakReference<Actor>>();
  private final WeakHashMap<Actor, Actor> mSenderToProxyMap = new WeakHashMap<Actor, Actor>();

  public ProxyScript(@NotNull final Actor actor) {
    mActor = ConstantConditions.notNull("actor", actor);
  }

  @NotNull
  public static Options asSentAt(final long sentAt, @NotNull final Options options) {
    return options.withTimeOffset(System.currentTimeMillis() - sentAt + options.getTimeOffset());
  }

  @NotNull
  @Override
  public final Behavior getBehavior(@NotNull final String id) {
    // TODO: 16/01/2019 full vs incoming
    return new Behavior() {

      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) throws Exception {
        final HashMap<Actor, WeakReference<Actor>> proxyToSenderMap = mProxyToSenderMap;
        final WeakHashMap<Actor, Actor> senderToProxyMap = mSenderToProxyMap;
        final Actor actor = mActor;
        final Actor sender = envelop.getSender();
        if (actor.equals(sender)) {
          if (message instanceof DeadLetter) {
            for (final Actor proxy : senderToProxyMap.values()) {
              proxy.dismiss(false);
            }
            context.dismissSelf();

          } else {
            // TODO: 16/01/2019 short circuit! bounce?
          }

        } else if (proxyToSenderMap.containsKey(sender)) {
          final Actor originalSender = proxyToSenderMap.get(sender).get();
          if (originalSender == null) {
            // TODO: 16/01/2019 bounce
            sender.dismiss(false);
            proxyToSenderMap.remove(sender);

          } else {
            onOutgoing(actor, originalSender, message, envelop.getSentAt(), envelop.getOptions(),
                context);
          }

        } else {
          final Actor self = context.getSelf();
          Actor proxy = senderToProxyMap.get(sender);
          if (proxy == null) {
            proxyToSenderMap.keySet().retainAll(senderToProxyMap.values());
            proxy = new LocalStage().newActor(sender.getId(), new SenderScript(self, mActor));
            senderToProxyMap.put(sender, proxy);
            proxyToSenderMap.put(proxy, new WeakReference<Actor>(sender));
          }
          onIncoming(actor, proxy, message, envelop.getSentAt(), envelop.getOptions(), context);
        }
        envelop.preventReceipt();
      }

      public void onStart(@NotNull final Context context) {
        mActor.addObserver(context.getSelf());
      }

      public void onStop(@NotNull final Context context) {
        mActor.removeObserver(context.getSelf());
      }
    };
  }

  protected void onIncoming(@NotNull final Actor proxied, @NotNull final Actor sender,
      final Object message, final long sentAt, @NotNull final Options options,
      @NotNull final Context context) throws Exception {
    proxied.tell(message, asSentAt(sentAt, options), sender);
  }

  protected void onOutgoing(@NotNull final Actor proxied, @NotNull final Actor recipient,
      final Object message, final long sentAt, @NotNull final Options options,
      @NotNull final Context context) throws Exception {
    recipient.tell(message, asSentAt(sentAt, options), context.getSelf());
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
      return new Behavior() {

        public void onMessage(final Object message, @NotNull final Envelop envelop,
            @NotNull final Context context) {
          final Actor proxy = mProxy;
          if (proxy.equals(envelop.getSender())) {
            mProxied.tell(message, asSentAt(envelop.getSentAt(), envelop.getOptions()),
                context.getSelf());

          } else {
            proxy.tell(message, asSentAt(envelop.getSentAt(), envelop.getOptions()),
                context.getSelf());
          }
          envelop.preventReceipt();
        }

        public void onStart(@NotNull final Context context) {
        }

        public void onStop(@NotNull final Context context) {
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
