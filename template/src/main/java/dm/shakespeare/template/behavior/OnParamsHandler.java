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
import java.util.ArrayList;
import java.util.List;

import dm.shakespeare.actor.Behavior.Agent;
import dm.shakespeare.actor.BehaviorBuilder;
import dm.shakespeare.actor.BehaviorBuilder.Handler;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.function.Tester;
import dm.shakespeare.template.behavior.annotation.OnParams;
import dm.shakespeare.template.config.BuildConfig;
import dm.shakespeare.util.ConstantConditions;
import dm.shakespeare.util.Iterables;

/**
 * {@code AnnotationHandler} handling {@link OnParams} annotations.
 */
class OnParamsHandler implements AnnotationHandler<OnParams> {

  private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

  public void handle(@NotNull final BehaviorBuilder builder, @NotNull final Object object,
      @NotNull final Method method, @NotNull final OnParams annotation) {
    builder.onMessage(new MessageTester(method), new MessageHandler(object, method));
  }

  private static class MessageHandler implements Handler<Iterable<?>>, Serializable {

    private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

    private final Method method;
    private final Object object;

    private MessageHandler() {
      object = null;
      method = null;
    }

    private MessageHandler(@NotNull final Object object, @NotNull final Method method) {
      this.object = object;
      this.method = ConstantConditions.notNull("method", method);
    }

    public Method getMethod() {
      return method;
    }

    public Object getObject() {
      return object;
    }

    public void handle(final Iterable<?> params, @NotNull final Envelop envelop,
        @NotNull final Agent agent) throws Exception {
      final Method method = this.method;
      final Class<?>[] parameterTypes = method.getParameterTypes();
      final int length = parameterTypes.length;
      List<?> args = Iterables.asList(params);
      final int size = args.size();
      if (length > size) {
        final ArrayList<Object> parameters = new ArrayList<Object>(args);
        for (int i = size; i < length; ++i) {
          final Class<?> parameterType = parameterTypes[i];
          if (parameterType == Envelop.class) {
            parameters.add(envelop);

          } else if (parameterType == Agent.class) {
            parameters.add(agent);

          } else {
            throw new IllegalArgumentException("invalid method parameter: " + parameterType);
          }
        }
        args = parameters;
      }
      final Object result = method.invoke(object, args.toArray());
      MethodHandler.handleReturnValue(method, result, envelop, agent);
    }
  }

  private static class MessageTester implements Tester<Object>, Serializable {

    private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

    private final Class<?>[] parameterTypes;

    private MessageTester() {
      parameterTypes = new Class<?>[0];
    }

    private MessageTester(@NotNull final Method method) {
      parameterTypes = method.getParameterTypes();
    }

    public Class<?>[] getParameterTypes() {
      return parameterTypes;
    }

    public boolean test(final Object message) {
      if (message instanceof Iterable) {
        final Class<?>[] parameterTypes = this.parameterTypes;
        final Iterable<?> iterable = (Iterable<?>) message;
        int i = 0;
        for (final Object o : iterable) {
          if ((o != null) && !parameterTypes[i++].isInstance(o)) {
            return false;
          }
        }
        return (i == parameterTypes.length);
      }
      return false;
    }
  }
}
