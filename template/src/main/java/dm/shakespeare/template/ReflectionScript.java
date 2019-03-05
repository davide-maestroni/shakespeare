/*
 * Copyright 2019 Davide Maestroni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dm.shakespeare.template;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import dm.shakespeare.actor.AbstractBehavior;
import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Behavior.Context;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.actor.Options;
import dm.shakespeare.actor.SerializableScript;
import dm.shakespeare.template.config.BuildConfig;
import dm.shakespeare.util.CQueue;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 01/17/2019.
 */
public class ReflectionScript extends SerializableScript {

  private static final ThreadLocal<CQueue<Context>> CONTEXTS =
      new ThreadLocal<CQueue<Context>>() {

        @Override
        protected CQueue<Context> initialValue() {
          return new CQueue<Context>();
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
  private static final ThreadLocal<CQueue<Envelop>> ENVELOPS =
      new ThreadLocal<CQueue<Envelop>>() {

        @Override
        protected CQueue<Envelop> initialValue() {
          return new CQueue<Envelop>();
        }
      };

  private static final long serialVersionUID = BuildConfig.VERSION_HASH_CODE;

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
              object.getClass().getMethod(invoke.getMethodName(), invoke.getParameterTypeArray()));

        } catch (final NoSuchMethodException e) {
          context.getLogger().wrn(e, "ignoring message: %s", message);
          return;
        }

        CONTEXTS.get().addFirst(context);
        ENVELOPS.get().addFirst(envelop);
        try {
          final Object result = method.invoke(object, invoke.getArgumentArray());
          final Class<?> returnType = method.getReturnType();
          if ((returnType != void.class) && (returnType != Void.class)) {
            // TODO: 31/08/2018 specific message?
            safeTell(envelop.getSender(), result, envelop.getOptions().threadOnly(), context);
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

    private final Object[] mArgumentArray;
    private final String mMethodName;
    private final Class<?>[] mParameterTypeArray;

    private Invoke(@NotNull final String methodName, @NotNull final Class<?>[] parameterTypes,
        @NotNull final Object... arguments) {
      mMethodName = ConstantConditions.notNull("methodName", methodName);
      mParameterTypeArray =
          ConstantConditions.notNullElements("parameterTypes", parameterTypes).clone();
      mArgumentArray = ConstantConditions.notNull("arguments", arguments).clone();
    }

    @NotNull
    public List<Object> getArguments() {
      return Collections.unmodifiableList(Arrays.asList(mArgumentArray));
    }

    @NotNull
    public String getMethodName() {
      return mMethodName;
    }

    @NotNull
    public List<Class<?>> getParameterTypes() {
      return Collections.unmodifiableList(Arrays.asList(mParameterTypeArray));
    }

    @NotNull
    private Object[] getArgumentArray() {
      return mArgumentArray;
    }

    @NotNull
    private Class<?>[] getParameterTypeArray() {
      return mParameterTypeArray;
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
