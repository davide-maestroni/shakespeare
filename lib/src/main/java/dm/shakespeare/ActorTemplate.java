package dm.shakespeare;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.ActorBuilder;
import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.BehaviorBuilder;
import dm.shakespeare.actor.Stage;
import dm.shakespeare.actor.ThreadMessage;
import dm.shakespeare.function.Mapper;
import dm.shakespeare.function.Provider;
import dm.shakespeare.log.Logger;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 09/10/2018.
 */
public abstract class ActorTemplate implements Actor {

  private static final AtomicLong sCount = new AtomicLong(Long.MIN_VALUE);

  private final String mId;
  private final Stage mStage;

  private volatile Actor mActor;

  // temp actor
  public ActorTemplate() {
    this(dm.shakespeare.BackStage.defaultInstance());
  }

  // temp actor
  public ActorTemplate(@NotNull final String id) {
    this(dm.shakespeare.BackStage.defaultInstance(), id);
  }

  // stage actor
  public ActorTemplate(@NotNull final Stage stage) {
    mStage = ConstantConditions.notNull("stage", stage);
    mId = getClass().getName() + "#" + sCount.getAndIncrement();
  }

  // stage actor
  public ActorTemplate(@NotNull final Stage stage, @NotNull final String id) {
    mStage = ConstantConditions.notNull("stage", stage);
    mId = ConstantConditions.notNull("id", id);
  }

  @NotNull
  protected static BehaviorBuilder newBehavior() {
    return new DefaultBehaviorBuilder();
  }

  @NotNull
  public ActorTemplate forward(final Object message, @NotNull final Envelop envelop,
      @NotNull final Actor sender) {
    getActor().forward(message, envelop, sender);
    return this;
  }

  @NotNull
  public final String getId() {
    return getActor().getId();
  }

  public boolean isStopped() {
    return getActor().isStopped();
  }

  public void kill() {
    getActor().kill();
  }

  @NotNull
  public ActorTemplate tell(final Object message, @NotNull final Actor sender) {
    getActor().tell(message, sender);
    return this;
  }

  @NotNull
  public ActorTemplate tellAll(@NotNull final Iterable<?> messages, @NotNull final Actor sender) {
    getActor().tellAll(messages, sender);
    return this;
  }

  @NotNull
  public <T> Conversation<T> thread(@NotNull final String threadId, @NotNull final Actor sender) {
    return getActor().thread(threadId, sender);
  }

  @NotNull
  public <T> Conversation<T> thread(@NotNull final String threadId, @NotNull final Actor sender,
      @NotNull final Class<? extends ThreadMessage>... messageFilters) {
    return getActor().thread(threadId, sender, messageFilters);
  }

  @NotNull
  public <T> Conversation<T> thread(@NotNull final String threadId, @NotNull final Actor sender,
      @NotNull final Collection<? extends Class<? extends ThreadMessage>> messageFilters) {
    return getActor().thread(threadId, sender, messageFilters);
  }

  @NotNull
  public final Stage getStage() {
    return mStage;
  }

  @NotNull
  public final ActorTemplate start() {
    getActor();
    return this;
  }

  @NotNull
  protected Behavior buildBehavior() throws Exception {
    return DefaultStage.defaultBehaviorProvider().get();
  }

  @NotNull
  protected ExecutorService buildExecutor() throws Exception {
    return DefaultStage.defaultExecutorMapper().apply(mId);
  }

  @NotNull
  protected Logger buildLogger() throws Exception {
    return DefaultStage.defaultLoggerMapper().apply(mId);
  }

  protected void init() throws Exception {
  }

  protected boolean mayInterruptIfRunning() throws Exception {
    return false;
  }

  protected boolean preventDefault() throws Exception {
    return false;
  }

  protected int quota() throws Exception {
    return Integer.MAX_VALUE;
  }

  @NotNull
  protected ActorTemplate reset() throws Exception {
    return ConstantConditions.unsupported("please provide your own implementation");
  }

  @NotNull
  private Actor getActor() {
    if (mActor == null) {
      mActor = mStage.getOrCreate(mId, new Mapper<ActorBuilder, Actor>() {

        public Actor apply(final ActorBuilder builder) throws Exception {
          init();
          return builder.behavior(new Provider<Behavior>() {

            private boolean mIsReset;

            public Behavior get() throws Exception {
              final ActorTemplate actor = mIsReset ? reset() : ActorTemplate.this;
              mIsReset = true;
              return actor.buildBehavior();
            }
          })
              .executor(new Mapper<String, ExecutorService>() {

                public ExecutorService apply(final String id) throws Exception {
                  return buildExecutor();
                }
              })
              .mayInterruptIfRunning(mayInterruptIfRunning())
              .preventDefault(preventDefault())
              .quota(quota())
              .logger(new Mapper<String, Logger>() {

                public Logger apply(final String value) throws Exception {
                  return buildLogger();
                }
              })
              .build();
        }
      });
    }

    return mActor;
  }
}
