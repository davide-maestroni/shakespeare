package dm.shakespeare2.template;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;

import dm.shakespeare.config.BuildConfig;
import dm.shakespeare.util.ConstantConditions;
import dm.shakespeare.util.DoubleQueue;
import dm.shakespeare2.actor.AbstractBehavior;
import dm.shakespeare2.actor.Actor;
import dm.shakespeare2.actor.ActorScript;
import dm.shakespeare2.actor.Behavior;
import dm.shakespeare2.actor.Behavior.Context;
import dm.shakespeare2.actor.Envelop;
import dm.shakespeare2.actor.Options;

/**
 * Created by davide-maestroni on 01/17/2019.
 */
public class ReflectionScript extends ActorScript {

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

  private final Object mObject;

  public ReflectionScript() {
    mObject = this;
  }

  public ReflectionScript(@NotNull final Object object) {
    mObject = ConstantConditions.notNull("object", object);
  }

  public static Context getContext() {
    return CONTEXTS.get().peekFirst();
  }

  public static Envelop getEnvelop() {
    return ENVELOPS.get().peekFirst();
  }

  @NotNull
  public static Invoke invoke(@NotNull final String methodName,
      @NotNull final Class<?>[] parameterTypes, @NotNull final Object... arguments) {
    return new Invoke(methodName, parameterTypes, arguments);
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public static <T> T tellAs(@NotNull final Class<? super T> type, @NotNull final Actor actor,
      @Nullable final Options options, @NotNull final Actor sender) {
    return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
        new ActorHandler(actor, options, sender));
  }

  @NotNull
  public Behavior getBehavior(@NotNull final String id) {
    return new AbstractBehavior() {

      public void onMessage(final Object message, @NotNull final Envelop envelop,
          @NotNull final Context context) throws Exception {
        if ((message == null) || (message.getClass() != Invoke.class)) {
          context.getLogger().wrn("ignoring message: %s", message);
          return;
        }

        final Object object = mObject;
        final Invoke invoke = (Invoke) message;
        final Method method;
        try {
          method = Methods.makeAccessible(
              object.getClass().getMethod(invoke.getMethodName(), invoke.getParameterTypes()));

        } catch (final NoSuchMethodException e) {
          context.getLogger().wrn(e, "ignoring message: %s", message);
          return;
        }

        CONTEXTS.get().addFirst(context);
        ENVELOPS.get().addFirst(envelop);
        try {
          final Object result = method.invoke(object, invoke.getArguments());
          final Class<?> returnType = method.getReturnType();
          if ((returnType != void.class) && (returnType != Void.class)) {
            // TODO: 31/08/2018 specific message?
            envelop.getSender()
                .tell(result, Options.thread(envelop.getOptions().getThread()), context.getSelf());
          }

        } finally {
          ENVELOPS.get().removeFirst();
          CONTEXTS.get().removeFirst();
        }
      }
    };
  }

  public static class Invoke implements Serializable {

    private static final long serialVersionUID = BuildConfig.VERSION_HASH_CODE;

    private final Object[] mArguments;
    private final String mMethodName;
    private final Class<?>[] mParameterTypes;

    private Invoke(@NotNull final String methodName, @NotNull final Class<?>[] parameterTypes,
        @NotNull final Object... arguments) {
      mMethodName = ConstantConditions.notNull("methodName", methodName);
      mParameterTypes =
          ConstantConditions.notNullElements("parameterTypes", parameterTypes).clone();
      mArguments = ConstantConditions.notNull("arguments", arguments).clone();
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

  private static class ActorHandler implements InvocationHandler {

    private final Actor mActor;
    private final Options mOptions;
    private final Actor mSender;

    private ActorHandler(@NotNull final Actor actor, @Nullable final Options options,
        @NotNull final Actor sender) {
      mActor = ConstantConditions.notNull("actor", actor);
      mSender = ConstantConditions.notNull("sender", sender);
      mOptions = options;
    }

    public Object invoke(final Object o, final Method method, final Object[] objects) {
      mActor.tell(new Invoke(method.getName(), method.getParameterTypes(), objects), mOptions,
          mSender);
      return DEFAULT_RETURN_VALUES.get(method.getReturnType());
    }
  }
}
