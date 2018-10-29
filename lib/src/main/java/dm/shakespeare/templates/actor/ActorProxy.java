package dm.shakespeare.templates.actor;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;

import dm.shakespeare.ActorTemplate;
import dm.shakespeare.Shakespeare;
import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Behavior.Context;
import dm.shakespeare.actor.BehaviorBuilder.Handler;
import dm.shakespeare.actor.Stage;
import dm.shakespeare.executor.ExecutorServices;
import dm.shakespeare.function.Mapper;
import dm.shakespeare.function.Provider;
import dm.shakespeare.function.Tester;

/**
 * Created by davide-maestroni on 10/10/2018.
 */
public abstract class ActorProxy extends ActorTemplate {

  private static final Tester<String> POSITIVE_TESTER = new Tester<String>() {

    public boolean test(final String value) {
      return true;
    }
  };

  private final ReferenceQueue<Actor> mReferenceQueue = new ReferenceQueue<Actor>();
  private final HashMap<Actor, ActorReference> mSenders = new HashMap<Actor, ActorReference>();

  public ActorProxy() {
  }

  public ActorProxy(@NotNull final String id) {
    super(id);
  }

  public ActorProxy(@NotNull final Stage stage) {
    super(stage);
  }

  public ActorProxy(@NotNull final Stage stage, @NotNull final String id) {
    super(stage, id);
  }

  @NotNull
  @Override
  protected final Behavior buildBehavior() {
    return newBehavior().onAny(new Handler<Object>() {

      public void handle(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) throws Exception {
        final Actor sender = envelop.getSender();
        ActorReference actorRef = mSenders.get(sender);
        Actor actor;
        if ((actorRef == null) || ((actor = actorRef.get()) == null)) {
          purgeSenders();
          actor = Shakespeare.backStage()
              .newActor()
              .id(sender.getId())
              .behavior(new Provider<Behavior>() {

                public Behavior get() {
                  return newBehavior().onAny(new Handler<Object>() {

                    public void handle(final Object message, @NotNull final Envelop envelop,
                        @NotNull final Context ignored) throws Exception {
                      ActorProxy.this.onOutgoingMessage(sender, message, envelop, context);
                    }
                  }).build();
                }
              })
              .executor(new Mapper<String, ExecutorService>() {

                public ExecutorService apply(final String value) {
                  return ExecutorServices.trampolineExecutor();
                }
              })
              .preventDefault(POSITIVE_TESTER)
              .build();
          mSenders.put(sender, new ActorReference(actor, sender, mReferenceQueue));
        }

        ActorProxy.this.onIncomingMessage(actor, message, envelop, context);
      }
    }).build();
  }

  @NotNull
  @Override
  protected final ExecutorService buildExecutor() {
    return ExecutorServices.trampolineExecutor();
  }

  @Override
  protected final boolean preventDefault() {
    return true;
  }

  @NotNull
  protected abstract Actor getProxied() throws Exception;

  protected void onIncomingMessage(@NotNull final Actor sender, final Object message,
      @NotNull final Envelop envelop, @NotNull final Context context) throws Exception {
    getProxied().forward(message, envelop, sender);
  }

  protected void onOutgoingMessage(@NotNull final Actor recipient, final Object message,
      @NotNull final Envelop envelop, @NotNull final Context context) throws Exception {
    recipient.forward(message, envelop, context.getSelf());
  }

  @SuppressWarnings("unchecked")
  private void purgeSenders() {
    @SuppressWarnings("UnnecessaryLocalVariable") final HashMap<Actor, ActorReference> senders =
        mSenders;
    @SuppressWarnings("UnnecessaryLocalVariable") final ReferenceQueue<Actor> referenceQueue =
        mReferenceQueue;
    Reference<? extends Actor> reference;
    while ((reference = referenceQueue.poll()) != null) {
      senders.remove(((ActorReference) reference).getSender());
    }
  }

  private static class ActorReference extends WeakReference<Actor> {

    private final Actor mSender;

    private ActorReference(@NotNull final Actor actor, @NotNull final Actor sender,
        @NotNull final ReferenceQueue<Actor> referenceQueue) {
      super(actor, referenceQueue);
      mSender = sender;
    }

    @NotNull
    private Actor getSender() {
      return mSender;
    }
  }
}
