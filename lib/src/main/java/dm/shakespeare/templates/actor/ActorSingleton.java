package dm.shakespeare.templates.actor;

import org.jetbrains.annotations.NotNull;

import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;

import dm.shakespeare.Shakespeare;
import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Behavior.Context;
import dm.shakespeare.actor.Stage;
import dm.shakespeare.function.Mapper;
import dm.shakespeare.function.Provider;
import dm.shakespeare.function.Tester;
import dm.shakespeare.log.Logger;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 09/24/2018.
 */
public abstract class ActorSingleton extends ActorProxy {

  private static final WeakHashMap<Class<? extends ActorSingleton>, Actor> sActors =
      new WeakHashMap<Class<? extends ActorSingleton>, Actor>();

  private final Stage mInstanceStage;

  private Actor mActor;

  public ActorSingleton(@NotNull final Stage stage) {
    super(Shakespeare.backStage());
    mInstanceStage = ConstantConditions.notNull("stage", stage);
  }

  public ActorSingleton(@NotNull final Stage stage, @NotNull final String id) {
    super(Shakespeare.backStage(), id);
    mInstanceStage = ConstantConditions.notNull("stage", stage);
  }

  @NotNull
  protected Behavior buildInstanceBehavior() throws Exception {
    return super.buildBehavior();
  }

  @NotNull
  protected ExecutorService buildInstanceExecutor() throws Exception {
    return super.buildExecutor();
  }

  @NotNull
  protected Logger buildInstanceLogger() throws Exception {
    return super.buildLogger();
  }

  @NotNull
  @Override
  protected final Actor getProxied() {
    return mActor;
  }

  @Override
  protected final void onIncomingMessage(@NotNull final Actor sender, final Object message,
      @NotNull final Envelop envelop, @NotNull final Context context) throws Exception {
    super.onIncomingMessage(sender, message, envelop, context);
  }

  @Override
  protected final void onOutgoingMessage(@NotNull final Actor recipient, final Object message,
      @NotNull final Envelop envelop, @NotNull final Context context) throws Exception {
    super.onOutgoingMessage(recipient, message, envelop, context);
  }

  @Override
  protected final void init() throws Exception {
    Actor actor = sActors.get(getClass());
    if (actor == null) {
      instanceInit();
      actor = mInstanceStage.newActor().id(getId()).behavior(new Provider<Behavior>() {

        public Behavior get() throws Exception {
          return buildInstanceBehavior();
        }
      }).executor(new Mapper<String, ExecutorService>() {

        public ExecutorService apply(final String value) throws Exception {
          return buildInstanceExecutor();
        }
      }).mayInterruptIfRunning(new Tester<String>() {

        public boolean test(final String value) throws Exception {
          return instanceMayInterruptIfRunning();
        }
      }).preventDefault(new Tester<String>() {

        public boolean test(final String value) throws Exception {
          return instancePreventDefault();
        }
      }).quota(new Mapper<String, Integer>() {

        public Integer apply(final String value) throws Exception {
          return instanceQuota();
        }
      }).logger(new Mapper<String, Logger>() {

        public Logger apply(final String value) throws Exception {
          return buildInstanceLogger();
        }
      }).build();
      sActors.put(getClass(), actor);
    }

    mActor = actor;
  }

  protected void instanceInit() throws Exception {
  }

  protected boolean instanceMayInterruptIfRunning() throws Exception {
    return super.mayInterruptIfRunning();
  }

  protected boolean instancePreventDefault() throws Exception {
    return false;
  }

  protected int instanceQuota() throws Exception {
    return super.quota();
  }
}
