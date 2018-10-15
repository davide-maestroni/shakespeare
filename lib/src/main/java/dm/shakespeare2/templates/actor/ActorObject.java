package dm.shakespeare2.templates.actor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;

import dm.shakespeare2.ActorTemplate;
import dm.shakespeare2.Shakespeare;
import dm.shakespeare2.actor.Actor;
import dm.shakespeare2.actor.Behavior;
import dm.shakespeare2.actor.Behavior.Context;
import dm.shakespeare2.actor.BehaviorBuilder;
import dm.shakespeare2.actor.BehaviorBuilder.Handler;
import dm.shakespeare2.actor.BehaviorBuilder.Matcher;
import dm.shakespeare2.actor.Stage;
import dm.shakespeare2.actor.ThreadMessage;
import dm.shakespeare2.function.Mapper;
import dm.shakespeare2.log.Logger;
import dm.shakespeare2.templates.behavior.BehaviorTemplates;
import dm.shakespeare2.templates.util.Methods;
import dm.shakespeare2.util.ConstantConditions;
import dm.shakespeare2.util.DoubleQueue;

/**
 * Created by davide-maestroni on 09/25/2018.
 */
public abstract class ActorObject extends ActorTemplate {

  private static final ThreadLocal<DoubleQueue<Actor>> CALLERS =
      new ThreadLocal<DoubleQueue<Actor>>() {

        @Override
        protected DoubleQueue<Actor> initialValue() {
          final DoubleQueue<Actor> actors = new DoubleQueue<Actor>();
          actors.add(Shakespeare.standIn());
          return actors;
        }
      };
  private static final ThreadLocal<DoubleQueue<Context>> CONTEXTS =
      new ThreadLocal<DoubleQueue<Context>>() {

        @Override
        protected DoubleQueue<Context> initialValue() {
          return new DoubleQueue<Context>();
        }
      };
  private static final HashMap<Class<?>, Object> DEFAULT_RETURN_VALUES =
      new HashMap<Class<?>, Object>() {{
        put(boolean.class, false);
        put(char.class, (char) 0);
        put(byte.class, (byte) 0);
        put(short.class, (short) 0);
        put(int.class, 0);
        put(long.class, 0L);
        put(float.class, (float) 0);
        put(double.class, (double) 0);
      }};
  private static final ThreadLocal<DoubleQueue<Envelop>> ENVELOPS =
      new ThreadLocal<DoubleQueue<Envelop>>() {

        @Override
        protected DoubleQueue<Envelop> initialValue() {
          return new DoubleQueue<Envelop>();
        }
      };

  private final ActorHandler mHandler;

  private Method mMethod;

  public ActorObject() {
    mHandler = new ActorHandler(this);
  }

  public ActorObject(@NotNull final String id) {
    super(id);
    mHandler = new ActorHandler(this);
  }

  public ActorObject(@NotNull final Stage stage) {
    super(stage);
    mHandler = new ActorHandler(this);
  }

  public ActorObject(@NotNull final Stage stage, @NotNull final String id) {
    super(stage, id);
    mHandler = new ActorHandler(this);
  }

  @NotNull
  public static ActorObjectBuilder newActor() {
    return newActor(Shakespeare.backStage());
  }

  @NotNull
  public static ActorObjectBuilder newActor(@NotNull final Stage stage) {
    return new ActorObjectBuilder(stage);
  }

  @NotNull
  protected static Context getContext() {
    return CONTEXTS.get().peekFirst();
  }

  @NotNull
  protected static Envelop getEnvelop() {
    return ENVELOPS.get().peekFirst();
  }

  @NotNull
  private static Behavior wrapWithContext(@NotNull final Behavior behavior) {
    if (behavior instanceof ContextBehavior) {
      return behavior;
    }

    return new ContextBehavior(behavior);
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public <T> T tellAs(@NotNull final Class<? super T> type) {
    return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, mHandler);
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public <T> T tellAs(@NotNull final Class<? super T> type, @NotNull final Actor sender) {
    return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
        new SenderActorHandler(this, ConstantConditions.notNull("sender", sender)));
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public <T> T threadAs(@NotNull final Class<? super T> type, @NotNull final String threadId,
      @NotNull final Actor sender) {
    final Conversation<Object> conversation = thread(threadId, sender);
    if (Closeable.class.isAssignableFrom(type)) {
      return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
          new CloseableConversationHandler(conversation));
    }

    return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type, Closeable.class},
        new ConversationHandler(conversation));
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public <T> T threadAs(@NotNull final Class<? super T> type, @NotNull final String threadId,
      @NotNull final Actor sender, @NotNull final Class<? extends ThreadMessage>... includes) {
    final Conversation<Object> conversation = thread(threadId, sender, includes);
    if (Closeable.class.isAssignableFrom(type)) {
      return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
          new CloseableConversationHandler(conversation));
    }

    return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type, Closeable.class},
        new ConversationHandler(conversation));
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public <T> T threadAs(@NotNull final Class<? super T> type, @NotNull final String threadId,
      @NotNull final Actor sender,
      @NotNull final Collection<? extends Class<? extends ThreadMessage>> includes) {
    final Conversation<Object> conversation = thread(threadId, sender, includes);
    if (Closeable.class.isAssignableFrom(type)) {
      return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
          new CloseableConversationHandler(conversation));
    }

    return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type, Closeable.class},
        new ConversationHandler(conversation));
  }

  @NotNull
  @Override
  protected final Behavior buildBehavior() throws Exception {
    final Object object = getObject();
    final Class<?> objectClass = object.getClass();
    return wrapWithContext(decorateBehavior(
        decorateBehavior(BehaviorTemplates.newBehavior(object).onMatch(new Matcher<Object>() {

          public boolean match(final Object message, @NotNull final Envelop envelop,
              @NotNull final Context context) {
            if ((message == null) || (message.getClass() != InvokeMessage.class)) {
              return false;
            }

            try {
              InvokeMessage invokeMessage = (InvokeMessage) message;
              mMethod = objectClass.getMethod(invokeMessage.getMethodName(),
                  invokeMessage.getParameterTypes());

            } catch (final NoSuchMethodException e) {
              context.getLogger().dbg(e, "ignoring message: %s", message);
              return false;
            }

            return true;
          }

        }, new Handler<InvokeMessage>() {

          public void handle(final InvokeMessage message, @NotNull final Envelop envelop,
              @NotNull final Context context) throws Exception {
            final Method method = Methods.makeAccessible(mMethod);
            final Object result = method.invoke(object, message.getArguments());
            final Class<?> returnType = method.getReturnType();
            if ((returnType != void.class) && (returnType != Void.class)) {
              // TODO: 31/08/2018 specific message?
              envelop.getSender().tell(result, context.getSelf());
            }
          }
        })).build()));
  }

  @NotNull
  protected BehaviorBuilder decorateBehavior(@NotNull final BehaviorBuilder builder) throws
      Exception {
    return builder;
  }

  @NotNull
  protected Behavior decorateBehavior(@NotNull final Behavior behavior) throws Exception {
    return behavior;
  }

  @NotNull
  protected Object getObject() {
    return this;
  }

  public static class ActorObjectBuilder {

    private final Stage mStage;

    private String mActorId;
    private Mapper<? super Behavior, ? extends Behavior> mBehaviorMapper;
    private Mapper<? super BehaviorBuilder, ? extends BehaviorBuilder> mBuilderMapper;
    private Mapper<? super String, ? extends ExecutorService> mExecutorMapper;
    private Mapper<? super String, ? extends Logger> mLoggerMapper;

    private ActorObjectBuilder(@NotNull final Stage stage) {
      mStage = ConstantConditions.notNull("stage", stage);
    }

    @NotNull
    public ActorObjectBuilder behavior(
        @NotNull final Mapper<? super Behavior, ? extends Behavior> mapper) {
      mBehaviorMapper = ConstantConditions.notNull("mapper", mapper);
      return this;
    }

    @NotNull
    public ActorObjectBuilder behaviorBuilder(
        @NotNull final Mapper<? super BehaviorBuilder, ? extends BehaviorBuilder> mapper) {
      mBuilderMapper = ConstantConditions.notNull("mapper", mapper);
      return this;
    }

    @NotNull
    public ActorObject build(@NotNull final Object object) {
      final String actorId = mActorId;
      return (actorId != null) ? new WrapperActorObject(object, mStage, actorId, mBehaviorMapper,
          mBuilderMapper, mExecutorMapper, mLoggerMapper)
          : new WrapperActorObject(object, mStage, mBehaviorMapper, mBuilderMapper, mExecutorMapper,
              mLoggerMapper);
    }

    @NotNull
    public ActorObjectBuilder executor(
        @NotNull final Mapper<? super String, ? extends ExecutorService> mapper) {
      mExecutorMapper = ConstantConditions.notNull("mapper", mapper);
      return this;
    }

    @NotNull
    public ActorObjectBuilder id(@NotNull final String id) {
      mActorId = ConstantConditions.notNull("mapper", id);
      return this;
    }

    @NotNull
    public ActorObjectBuilder logger(
        @NotNull final Mapper<? super String, ? extends Logger> mapper) {
      mLoggerMapper = ConstantConditions.notNull("mapper", mapper);
      return this;
    }
  }

  private static class ActorHandler implements InvocationHandler {

    private final Actor mActor;

    private ActorHandler(@NotNull final Actor actor) {
      mActor = actor;
    }

    public Object invoke(final Object o, final Method method, final Object[] objects) {
      mActor.tell(new InvokeMessage(method.getName(), method.getParameterTypes(), objects),
          CALLERS.get().peekFirst());
      return DEFAULT_RETURN_VALUES.get(method.getReturnType());
    }
  }

  private static class CloseableConversationHandler implements InvocationHandler {

    private final Conversation<Object> mConversation;

    private CloseableConversationHandler(@NotNull final Conversation<Object> conversation) {
      mConversation = conversation;
    }

    public Object invoke(final Object o, final Method method, final Object[] objects) {
      mConversation.tell(new InvokeMessage(method.getName(), method.getParameterTypes(), objects));
      return DEFAULT_RETURN_VALUES.get(method.getReturnType());
    }
  }

  private static class ContextBehavior implements Behavior {

    private final Behavior mBehavior;

    private ContextBehavior(@NotNull final Behavior behavior) {
      mBehavior = ConstantConditions.notNull("behavior", behavior);
    }

    public void message(final Object message, @NotNull final Envelop envelop,
        @NotNull final Context context) throws Exception {
      CALLERS.get().addFirst(context.getSelf());
      CONTEXTS.get().addFirst(context);
      ENVELOPS.get().addFirst(envelop);
      try {
        mBehavior.message(message, envelop, context);

      } finally {
        ENVELOPS.get().removeFirst();
        CONTEXTS.get().removeFirst();
        CALLERS.get().removeFirst();
      }
    }

    public void start(@NotNull final Context context) throws Exception {
      CALLERS.get().addFirst(context.getSelf());
      CONTEXTS.get().addFirst(context);
      try {
        mBehavior.start(context);

      } finally {
        CONTEXTS.get().removeFirst();
        CALLERS.get().removeFirst();
      }
    }

    public void stop(@NotNull final Context context) throws Exception {
      CALLERS.get().addFirst(context.getSelf());
      CONTEXTS.get().addFirst(context);
      try {
        mBehavior.stop(context);

      } finally {
        CONTEXTS.get().removeFirst();
        CALLERS.get().removeFirst();
      }
    }
  }

  private static class ConversationHandler implements InvocationHandler {

    private final Conversation<Object> mConversation;

    private ConversationHandler(@NotNull final Conversation<Object> conversation) {
      mConversation = conversation;
    }

    public Object invoke(final Object o, final Method method, final Object[] objects) {
      if (Closeable.class.getDeclaredMethods()[0].equals(method)) {
        mConversation.close();
        return null;
      }

      mConversation.tell(new InvokeMessage(method.getName(), method.getParameterTypes(), objects));
      return DEFAULT_RETURN_VALUES.get(method.getReturnType());
    }
  }

  private static class InvokeMessage {

    private final Object[] mArguments;
    private final String mMethodName;
    private final Class<?>[] mParameterTypes;

    private InvokeMessage(@NotNull final String methodName,
        @NotNull final Class<?>[] parameterTypes, @NotNull final Object... arguments) {
      mMethodName = methodName;
      mParameterTypes = parameterTypes;
      mArguments = arguments;
    }

    @NotNull
    private Object[] getArguments() {
      return mArguments;
    }

    @NotNull
    private String getMethodName() {
      return mMethodName;
    }

    @NotNull
    private Class<?>[] getParameterTypes() {
      return mParameterTypes;
    }
  }

  private static class SenderActorHandler implements InvocationHandler {

    private final Actor mActor;
    private final Actor mSender;

    private SenderActorHandler(@NotNull final Actor actor, @NotNull final Actor sender) {
      mActor = actor;
      mSender = sender;
    }

    public Object invoke(final Object o, final Method method, final Object[] objects) {
      mActor.tell(new InvokeMessage(method.getName(), method.getParameterTypes(), objects),
          mSender);
      return DEFAULT_RETURN_VALUES.get(method.getReturnType());
    }
  }

  private static class WrapperActorObject extends ActorObject {

    private final Mapper<? super Behavior, ? extends Behavior> mBehaviorMapper;
    private final Mapper<? super BehaviorBuilder, ? extends BehaviorBuilder> mBuilderMapper;
    private final Mapper<? super String, ? extends ExecutorService> mExecutorMapper;
    private final Mapper<? super String, ? extends Logger> mLoggerMapper;
    private final Object mObject;

    private WrapperActorObject(@NotNull final Object object, @NotNull final Stage stage,
        @Nullable final Mapper<? super Behavior, ? extends Behavior> behaviorMapper,
        @Nullable final Mapper<? super BehaviorBuilder, ? extends BehaviorBuilder> builderMapper,
        @Nullable final Mapper<? super String, ? extends ExecutorService> executorMapper,
        @Nullable final Mapper<? super String, ? extends Logger> loggerMapper) {
      super(stage);
      mObject = ConstantConditions.notNull("object", object);
      mBehaviorMapper = behaviorMapper;
      mBuilderMapper = builderMapper;
      mExecutorMapper = executorMapper;
      mLoggerMapper = loggerMapper;
    }

    private WrapperActorObject(@NotNull final Object object, @NotNull final Stage stage,
        @NotNull final String id,
        @Nullable final Mapper<? super Behavior, ? extends Behavior> behaviorMapper,
        @Nullable final Mapper<? super BehaviorBuilder, ? extends BehaviorBuilder> builderMapper,
        @Nullable final Mapper<? super String, ? extends ExecutorService> executorMapper,
        @Nullable final Mapper<? super String, ? extends Logger> loggerMapper) {
      super(stage, id);
      mObject = ConstantConditions.notNull("object", object);
      mBehaviorMapper = behaviorMapper;
      mBuilderMapper = builderMapper;
      mExecutorMapper = executorMapper;
      mLoggerMapper = loggerMapper;
    }

    @NotNull
    @Override
    protected BehaviorBuilder decorateBehavior(@NotNull final BehaviorBuilder builder) throws
        Exception {
      final Mapper<? super BehaviorBuilder, ? extends BehaviorBuilder> builderMapper =
          mBuilderMapper;
      return (builderMapper != null) ? builderMapper.apply(builder) : builder;
    }

    @NotNull
    @Override
    protected Behavior decorateBehavior(@NotNull final Behavior behavior) throws Exception {
      final Mapper<? super Behavior, ? extends Behavior> behaviorMapper = mBehaviorMapper;
      return (behaviorMapper != null) ? behaviorMapper.apply(behavior) : behavior;
    }

    @NotNull
    @Override
    public Object getObject() {
      return mObject;
    }

    @NotNull
    @Override
    protected ActorTemplate reset() {
      return this;
    }

    @NotNull
    @Override
    protected ExecutorService buildExecutor() throws Exception {
      final Mapper<? super String, ? extends ExecutorService> executorMapper = mExecutorMapper;
      return (executorMapper != null) ? executorMapper.apply(getId()) : super.buildExecutor();
    }

    @NotNull
    @Override
    protected Logger buildLogger() throws Exception {
      final Mapper<? super String, ? extends Logger> loggerMapper = mLoggerMapper;
      return (loggerMapper != null) ? loggerMapper.apply(getId()) : super.buildLogger();
    }
  }
}
