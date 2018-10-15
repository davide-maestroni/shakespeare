package dm.shakespeare2.templates.actor;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;

import dm.shakespeare2.ActorTemplate;
import dm.shakespeare2.Shakespeare;
import dm.shakespeare2.actor.Actor;
import dm.shakespeare2.actor.Behavior;
import dm.shakespeare2.actor.Behavior.Context;
import dm.shakespeare2.actor.BehaviorBuilder.Handler;
import dm.shakespeare2.actor.Stage;
import dm.shakespeare2.function.Mapper;
import dm.shakespeare2.function.Provider;
import dm.shakespeare2.log.Logger;
import dm.shakespeare2.message.ActorStoppedMessage;
import dm.shakespeare2.message.AddActorMonitorMessage;
import dm.shakespeare2.util.ConstantConditions;
import dm.shakespeare2.util.DoubleQueue;

/**
 * Created by davide-maestroni on 09/24/2018.
 */
public abstract class ActorFactory extends ActorProxy {

  private static final Object DUMMY_MESSAGE = new Object();
  private static final Object STOP_MESSAGE = new Object();

  private final FactoryOrchestrator mOrchestrator;
  private final Stage mStage;
  private Actor mActor;

  private DoubleQueue<DelayedMessage> mDelayedMessages = new DoubleQueue<DelayedMessage>();
  private IncomingHandler mHandler = new InitialHandler();

  public ActorFactory(@NotNull final FactoryOrchestrator orchestrator) {
    this(Shakespeare.backStage(), orchestrator); // TODO: 15/10/2018 factory stage
  }

  public ActorFactory(@NotNull final Stage stage, @NotNull final FactoryOrchestrator orchestrator) {
    super(Shakespeare.backStage());
    mStage = ConstantConditions.notNull("stage", stage);
    mOrchestrator = ConstantConditions.notNull("orchestrator", orchestrator);
  }

  public ActorFactory(@NotNull final Stage stage, @NotNull final String id,
      @NotNull final FactoryOrchestrator orchestrator) {
    super(Shakespeare.backStage(), id);
    mStage = ConstantConditions.notNull("stage", stage);
    mOrchestrator = ConstantConditions.notNull("orchestrator", orchestrator);
  }

  public ActorFactory(@NotNull final String id, @NotNull final FactoryOrchestrator orchestrator) {
    this(Shakespeare.backStage(), id, orchestrator);
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
  public static FactoryOrchestrator newFactoryOrchestrator(final int maxConcurrentInstances,
      @NotNull final Stage stage) {
    return new FactoryOrchestrator(maxConcurrentInstances, stage);
  }

  @NotNull
  public static FactoryOrchestrator newFactoryOrchestrator(@NotNull final Stage stage) {
    return newFactoryOrchestrator(Integer.MAX_VALUE, stage);
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
      mActor = mStage.newActor()
          .id(getId())
          .behavior(new BehaviorProvider((BehaviorMessage) message))
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
          .build()
          .tell(AddActorMonitorMessage.defaultInstance(), context.getSelf());
      // TODO: 15/10/2018 wrap behavior to detect stop instead of monitor
      mHandler = new ConsumeHandler();
      final int size = mDelayedMessages.size();
      for (int i = 0; i < size; ++i) {
        tell(DUMMY_MESSAGE, ActorFactory.this);
      }

    } else if ((message instanceof ActorStoppedMessage) && envelop.getSender().equals(mActor)) {
      mActor = null;
      mOrchestrator.tell(STOP_MESSAGE, context.getSelf());

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
            envelop.getSender()
                .tell(new BehaviorMessage(message.buildInstanceBehavior()), context.getSelf());

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
            factory.tell(new BehaviorMessage(factory.buildInstanceBehavior()), context.getSelf());

          } else {
            --mInstanceCount;
          }
        }
      }).build();
    }
  }

  private static class BehaviorMessage {

    private final Behavior mBehavior;

    private BehaviorMessage(@NotNull final Behavior behavior) {
      mBehavior = ConstantConditions.notNull("behavior", behavior);
    }

    @NotNull
    public Behavior getBehavior() {
      return mBehavior;
    }
  }

  private static class BehaviorProvider implements Provider<Behavior> {

    private final BehaviorMessage mMessage;

    private BehaviorProvider(@NotNull final BehaviorMessage message) {
      mMessage = message;
    }

    public Behavior get() {
      return mMessage.getBehavior();
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

  private class ConsumeHandler implements IncomingHandler {

    public void handle(@NotNull final Actor sender, final Object message,
        @NotNull final Envelop envelop, @NotNull final Context context) throws Exception {
      final DoubleQueue<DelayedMessage> delayedMessages = mDelayedMessages;
      if (!delayedMessages.isEmpty()) {
        final DelayedMessage delayedMessage = delayedMessages.removeFirst();
        if (message != DUMMY_MESSAGE) {
          delayedMessages.add(new DelayedMessage(sender, message, envelop));
        }

        ActorFactory.super.onIncomingMessage(delayedMessage.getSender(),
            delayedMessage.getMessage(), delayedMessage.getEnvelop(), context);

      } else {
        mDelayedMessages = new DoubleQueue<DelayedMessage>();
        mHandler = new DefaultHandler();
        ActorFactory.super.onIncomingMessage(sender, message, envelop, context);
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
