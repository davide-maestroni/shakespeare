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

package dm.shakespeare.template.behavior;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import dm.shakespeare.actor.Envelop;
import dm.shakespeare.actor.SerializableAbstractBehavior;
import dm.shakespeare.template.config.BuildConfig;
import dm.shakespeare.template.util.Reflections;
import dm.shakespeare.util.CQueue;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 03/24/2019.
 */
public class ReflectionBehavior extends SerializableAbstractBehavior {

  private static final ThreadLocal<CQueue<Agent>> agents = new ThreadLocal<CQueue<Agent>>() {

    @Override
    protected CQueue<Agent> initialValue() {
      return new CQueue<Agent>();
    }
  };
  private static final ThreadLocal<CQueue<Envelop>> envelops = new ThreadLocal<CQueue<Envelop>>() {

    @Override
    protected CQueue<Envelop> initialValue() {
      return new CQueue<Envelop>();
    }
  };
  private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

  private final Object object;

  ReflectionBehavior(@NotNull final Object object) {
    this.object = ConstantConditions.notNull("object", object);
  }

  public static Agent getAgent() {
    return agents.get().peekFirst();
  }

  public static Envelop getEnvelop() {
    return envelops.get().peekFirst();
  }

  @NotNull
  public static Invoke invoke(@NotNull final String methodName,
      @NotNull final Class<?>[] parameterTypes, @NotNull final Object... arguments) {
    return new Invoke(methodName, parameterTypes, arguments);
  }

  public void onMessage(final Object message, @NotNull final Envelop envelop,
      @NotNull final Agent agent) throws Exception {
    if ((message == null) || (message.getClass() != Invoke.class)) {
      agent.getLogger().wrn("ignoring message: %s", message);
      return;
    }
    final Object object = this.object;
    final Invoke invoke = (Invoke) message;
    final Method method;
    try {
      method = Reflections.makeAccessible(
          object.getClass().getMethod(invoke.getMethodName(), invoke.getParameterTypeArray()));

    } catch (final NoSuchMethodException e) {
      agent.getLogger().wrn(e, "ignoring message: %s", message);
      return;
    }
    agents.get().addFirst(agent);
    envelops.get().addFirst(envelop);
    try {
      final Object result = method.invoke(object, invoke.getArgumentArray());
      final Class<?> returnType = method.getReturnType();
      if ((returnType != void.class) && (returnType != Void.class)) {
        try {
          // TODO: 31/08/2018 specific message?
          envelop.getSender().tell(result, envelop.getHeaders().threadOnly(), agent.getSelf());

        } catch (final RejectedExecutionException e) {
          agent.getLogger().err(e, "ignoring exception");
        }
      }

    } finally {
      envelops.get().removeFirst();
      agents.get().removeFirst();
    }
  }

  public static class Invoke implements Serializable {

    private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

    private final Object[] argumentArray;
    private final String methodName;
    private final Class<?>[] parameterTypeArray;

    private Invoke(@NotNull final String methodName, @NotNull final Class<?>[] parameterTypes,
        @NotNull final Object... arguments) {
      this.methodName = ConstantConditions.notNull("methodName", methodName);
      parameterTypeArray =
          ConstantConditions.notNullElements("parameterTypes", parameterTypes).clone();
      argumentArray = ConstantConditions.notNull("arguments", arguments).clone();
    }

    @NotNull
    public List<Object> getArguments() {
      return Collections.unmodifiableList(Arrays.asList(argumentArray));
    }

    @NotNull
    public String getMethodName() {
      return methodName;
    }

    @NotNull
    public List<Class<?>> getParameterTypes() {
      return Collections.unmodifiableList(Arrays.asList(parameterTypeArray));
    }

    @NotNull
    private Object[] getArgumentArray() {
      return argumentArray;
    }

    @NotNull
    private Class<?>[] getParameterTypeArray() {
      return parameterTypeArray;
    }
  }
}
