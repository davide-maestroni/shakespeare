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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import dm.shakespeare.actor.Behavior.Context;
import dm.shakespeare.actor.BehaviorBuilder;
import dm.shakespeare.actor.BehaviorBuilder.Handler;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.function.Tester;
import dm.shakespeare.template.annotation.OnParams;
import dm.shakespeare.util.Iterables;

/**
 * Created by davide-maestroni on 09/07/2018.
 */
class OnParamsHandler implements AnnotationHandler<OnParams> {

  public void handle(@NotNull final BehaviorBuilder builder, @NotNull final Object object,
      @NotNull final Method method, @NotNull final OnParams annotation) {
    builder.onMessage(new MessageTester(method), new MessageHandler(object, method));
  }

  private static class MessageHandler implements Handler<Iterable<?>> {

    private final Method mMethod;
    private final Object mObject;

    private MessageHandler(@NotNull final Object object, @NotNull final Method method) {
      mObject = object;
      mMethod = Methods.makeAccessible(method);
    }

    public void handle(final Iterable<?> params, @NotNull final Envelop envelop,
        @NotNull final Context context) throws Exception {
      final Method method = mMethod;
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

          } else if (parameterType == Context.class) {
            parameters.add(context);

          } else {
            throw new IllegalArgumentException("invalid method parameter: " + parameterType);
          }
        }
        args = parameters;
      }
      final Object result = method.invoke(mObject, args.toArray());
      MethodHandler.handleReturnValue(method, result, envelop, context);
    }
  }

  private static class MessageTester implements Tester<Object> {

    private final Class<?>[] mParameterTypes;

    private MessageTester(@NotNull final Method method) {
      mParameterTypes = method.getParameterTypes();
    }

    public boolean test(final Object message) {
      if (message instanceof Iterable) {
        final Class<?>[] parameterTypes = mParameterTypes;
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
