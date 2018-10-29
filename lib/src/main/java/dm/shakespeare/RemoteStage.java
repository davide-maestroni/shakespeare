package dm.shakespeare;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Actor.ActorSet;
import dm.shakespeare.actor.Actor.Envelop;
import dm.shakespeare.actor.ActorBuilder;
import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Behavior.Context;
import dm.shakespeare.actor.BehaviorBuilder.Handler;
import dm.shakespeare.executor.ExecutorServices;
import dm.shakespeare.function.Mapper;
import dm.shakespeare.function.Observer;
import dm.shakespeare.function.Provider;
import dm.shakespeare.function.Tester;
import dm.shakespeare.remote.RemoteAdapter;
import dm.shakespeare.remote.RemoteAdapter.StageDescription;

/**
 * Created by davide-maestroni on 10/17/2018.
 */
class RemoteStage extends DefaultStage {

  private static final Object STOP_MESSAGE = new Object();

  private final RemoteAdapter mAdapter;
  private final Object mMutex = new Object();

  private Map<String, String> mCapabilities = Collections.emptyMap();

  RemoteStage(@NotNull final String name, @NotNull final RemoteAdapter adapter) throws Exception {
    super(name);
    (mAdapter = adapter).describeStage(new Observer<StageDescription>() {

      public void accept(final StageDescription description) {
        final Map<String, String> capabilities = description.getCapabilities();
        synchronized (mMutex) {
          if (capabilities != null) {
            mCapabilities = new HashMap<String, String>(capabilities);

          } else {
            mCapabilities = Collections.emptyMap();
          }
        }

        final Collection<String> actors = description.getActors();
        if (actors != null) {
          for (final String actorId : actors) {
            //            RemoteStage.super.getOrCreate(actorId, new ActorBuilderMapper(actorId));
          }

          // stop removed actors
          final ActorSet actorSet = findAll(new ActorsTester(actors));
          actorSet.tell(STOP_MESSAGE, StandInActor.defaultInstance());

        } else {
          // stop all actors
          final ActorSet actorSet = getAll();
          actorSet.tell(STOP_MESSAGE, StandInActor.defaultInstance());
        }
      }
    });
  }

  @NotNull
  public ActorBuilder newActor() {
    verifyCanCreate();
    return null;
  }

  private void verifyCanCreate() {
    synchronized (mMutex) {
      final String actorCreate = mCapabilities.get("actor.create");
      if (!"enabled".equalsIgnoreCase(actorCreate)) {
        throw new UnsupportedOperationException();
      }
    }
  }

  private static class ActorsTester implements Tester<Actor> {

    private final Collection<String> mActors;

    private ActorsTester(@NotNull final Collection<String> actors) {
      mActors = actors;
    }

    public boolean test(final Actor actor) {
      return !mActors.contains(actor.getId());
    }
  }

  private class ActorBuilderMapper implements Mapper<ActorBuilder, Actor> {

    private final String mActorId;

    private ActorBuilderMapper(@NotNull final String actorId) {
      mActorId = actorId;
    }

    public Actor apply(final ActorBuilder builder) {
      return builder.behavior(new Provider<Behavior>() {

        public Behavior get() {
          return new DefaultBehaviorBuilder().onMessageEqualTo(STOP_MESSAGE, new Handler<Object>() {

            public void handle(final Object message, @NotNull final Envelop envelop,
                @NotNull final Context context) {
              context.stopSelf();
            }
          }).onNoMatch(new Handler<Object>() {

            public void handle(final Object message, @NotNull final Envelop envelop,
                @NotNull final Context context) throws Exception {
              mAdapter.sendMessage(mActorId, message, envelop);
            }
          }).build();
        }
      }).executor(new Mapper<String, ExecutorService>() {

        public ExecutorService apply(final String value) {
          return ExecutorServices.trampolineExecutor(); // TODO: 17/10/2018 trampoline???
        }
      })
          //          .preventDefault(true)
          .build();
      // TODO: 17/10/2018 quota, interrupt???
    }
  }
}
