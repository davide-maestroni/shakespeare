package dm.shakespeare.templates.actor;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;

import dm.shakespeare.ActorTemplate;
import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Behavior.Context;
import dm.shakespeare.actor.BehaviorBuilder.Handler;
import dm.shakespeare.actor.Stage;
import dm.shakespeare.function.Mapper;
import dm.shakespeare.function.Provider;
import dm.shakespeare.log.Logger;
import dm.shakespeare.util.ConstantConditions;
import dm.shakespeare.util.DoubleQueue;

/**
 * Created by davide-maestroni on 09/24/2018.
 */
public abstract class ActorFactory extends ActorProxy {

  private static final Object STOP_MESSAGE = new Object();

  private final Stage mInstanceStage;
  private final FactoryOrchestrator mOrchestrator;

  private Actor mActor;
  private DoubleQueue<DelayedMessage> mDelayedMessages = new DoubleQueue<DelayedMessage>();
  private IncomingHandler mHandler = new InitialHandler();

  public ActorFactory(@NotNull final FactoryOrchestrator orchestrator,
      @NotNull final Stage factoryStage, @NotNull final Stage instanceStage) {
    super(factoryStage);
    mInstanceStage = ConstantConditions.notNull("stage", instanceStage);
    mOrchestrator = ConstantConditions.notNull("orchestrator", orchestrator);
  }

  public ActorFactory(@NotNull final FactoryOrchestrator orchestrator,
      @NotNull final Stage factoryStage, @NotNull final Stage instanceStage,
      @NotNull final String id) {
    super(factoryStage, id);
    mInstanceStage = ConstantConditions.notNull("stage", instanceStage);
    mOrchestrator = ConstantConditions.notNull("orchestrator", orchestrator);
  }

  @NotNull
  public static FactoryOrchestrator newFactoryOrchestrator() {
    return newFactoryOrchestrator(Integer.MAX_VALUE);
  }

  @NotNull
  public static FactoryOrchestrator newFactoryOrchestrator(final int maxConcurrentInstances) {
    return new FactoryOrchestrator(maxConcurrentInstances);
  }

  @NotNull
  public static FactoryOrchestrator newFactoryOrchestrator(@NotNull final Stage stage) {
    return newFactoryOrchestrator(stage, Integer.MAX_VALUE);
  }

  @NotNull
  public static FactoryOrchestrator newFactoryOrchestrator(@NotNull final Stage stage,
      final int maxConcurrentInstances) {
    return new FactoryOrchestrator(maxConcurrentInstances, stage);
  }

  protected void beforeInit() throws Exception {
    super.init();
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
  protected void onIncomingMessage(@NotNull final Actor sender, final Object message,
      @NotNull final Envelop envelop, @NotNull final Context context) throws Exception {
    if (message instanceof BehaviorMessage) {
      mActor = mInstanceStage.newActor()
          .id(getId())
          .behavior((BehaviorMessage) message)
          .executor(new Mapper<String, ExecutorService>() {

            public ExecutorService apply(final String value) throws Exception {
              return buildInstanceExecutor();
            }
          })
          .mayInterruptIfRunning(instanceMayInterruptIfRunning())
          .preventDefault(instancePreventDefault())
          .quota(instanceQuota())
          .logger(new Mapper<String, Logger>() {

            public Logger apply(final String value) throws Exception {
              return buildInstanceLogger();
            }
          })
          .build();
      mHandler = new DefaultHandler();
      for (final DelayedMessage delayedMessage : mDelayedMessages) {
        super.onIncomingMessage(delayedMessage.getSender(), delayedMessage.getMessage(),
            delayedMessage.getEnvelop(), context);
      }

      mDelayedMessages = new DoubleQueue<DelayedMessage>();

    } else {
      mHandler.handle(sender, message, envelop, context);
    }
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

  private interface IncomingHandler {

    void handle(@NotNull Actor sender, Object message, @NotNull Envelop envelop,
        @NotNull Context context) throws Exception;
  }

  public static class FactoryOrchestrator extends ActorTemplate {

    private final DoubleQueue<ActorFactory> mFactories = new DoubleQueue<ActorFactory>();
    private final int mMaxInstances;

    private int mInstanceCount;

    private FactoryOrchestrator(final int maxConcurrentInstances) {
      mMaxInstances = ConstantConditions.positive("maxConcurrentInstances", maxConcurrentInstances);
    }

    private FactoryOrchestrator(final int maxConcurrentInstances, @NotNull final Stage stage) {
      super(stage);
      mMaxInstances = ConstantConditions.positive("maxConcurrentInstances", maxConcurrentInstances);
    }

    @NotNull
    @Override
    protected Behavior buildBehavior() {
      return newBehavior().onMessage(ActorFactory.class, new Handler<ActorFactory>() {

        public void handle(final ActorFactory message, @NotNull final Envelop envelop,
            @NotNull final Context context) throws Exception {
          if (mInstanceCount < mMaxInstances) {
            ++mInstanceCount;
            final Actor self = context.getSelf();
            envelop.getSender()
                .tell(new BehaviorMessage(message.buildInstanceBehavior(), self), self);

          } else {
            mFactories.add(message);
          }
        }
      }).onMessageEqualTo(STOP_MESSAGE, new Handler<Object>() {

        public void handle(final Object message, @NotNull final Envelop envelop,
            @NotNull final Context context) throws Exception {
          final DoubleQueue<ActorFactory> factories = mFactories;
          if (!factories.isEmpty()) {
            final ActorFactory factory = factories.removeFirst();
            final Actor self = context.getSelf();
            factory.tell(new BehaviorMessage(factory.buildInstanceBehavior(), self), self);

          } else {
            --mInstanceCount;
          }
        }
      }).build();
    }
  }

  private static class BehaviorMessage implements Provider<Behavior> {

    private final Behavior mBehavior;
    private final Actor mOrchestrator;

    private BehaviorMessage(@NotNull final Behavior behavior, @NotNull final Actor orchestrator) {
      mBehavior = ConstantConditions.notNull("behavior", behavior);
      mOrchestrator = orchestrator;
    }

    public Behavior get() {
      return new FactoryBehavior(mOrchestrator, mBehavior);
    }
  }

  private static class DelayedMessage {

    private final Envelop mEnvelop;
    private final Object mMessage;
    private final Actor mSender;

    private DelayedMessage(@NotNull final Actor sender, final Object message,
        @NotNull final Envelop envelop) {
      mSender = sender;
      mMessage = message;
      mEnvelop = envelop;
    }

    @NotNull
    public Actor getSender() {
      return mSender;
    }

    @NotNull
    Envelop getEnvelop() {
      return mEnvelop;
    }

    Object getMessage() {
      return mMessage;
    }
  }

  private static class FactoryBehavior implements Behavior {

    private final Behavior mBehavior;
    private final Actor mOrchestrator;

    private FactoryBehavior(@NotNull final Actor orchestrator, @NotNull final Behavior behavior) {
      mOrchestrator = orchestrator;
      mBehavior = behavior;
    }

    public void message(final Object message, @NotNull final Envelop envelop,
        @NotNull final Context context) throws Exception {
      mBehavior.message(message, envelop, context);
    }

    public void start(@NotNull final Context context) throws Exception {
      mBehavior.start(context);
    }

    public void stop(@NotNull final Context context) throws Exception {
      try {
        mBehavior.stop(context);

      } finally {
        mOrchestrator.tell(STOP_MESSAGE, context.getSelf());
      }
    }
  }

  private class DefaultHandler implements IncomingHandler {

    public void handle(@NotNull final Actor sender, final Object message,
        @NotNull final Envelop envelop, @NotNull final Context context) throws Exception {
      ActorFactory.super.onIncomingMessage(sender, message, envelop, context);
    }
  }

  private class InitialHandler implements IncomingHandler {

    public void handle(@NotNull final Actor sender, final Object message,
        @NotNull final Envelop envelop, @NotNull final Context context) {
      mDelayedMessages.add(new DelayedMessage(sender, message, envelop));
    }
  }

  @Override
  protected final void init() throws Exception {
    beforeInit();
    mOrchestrator.tell(this, this);
  }
}
