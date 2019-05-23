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

package dm.shakespeare.template.actor;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.ArrayList;

import dm.shakespeare.actor.Behavior.Agent;
import dm.shakespeare.actor.BehaviorBuilder.Handler;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.template.util.Reflections;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 09/12/2018.
 */
class MethodHandler implements Handler<Object> {

  private final Method method;
  private final Object object;

  MethodHandler(@NotNull final Object object, @NotNull final Method method) {
    this.object = ConstantConditions.notNull("object", object);
    this.method = Reflections.makeAccessible(method);
  }

  static void handleReturnValue(@NotNull final Method method, final Object value,
      @NotNull final Envelop envelop, @NotNull final Agent agent) {
    final Class<?> returnType = method.getReturnType();
    if ((returnType != void.class) && (returnType != Void.class)) {
      // TODO: 31/08/2018 specific message?
      envelop.getSender().tell(value, envelop.getOptions().threadOnly(), agent.getSelf());
    }
  }

  public void handle(final Object message, @NotNull final Envelop envelop,
      @NotNull final Agent agent) throws Exception {
    final Method method = this.method;
    final Class<?>[] parameterTypes = method.getParameterTypes();
    final int length = parameterTypes.length;
    final Object[] args;
    if (length > 1) {
      final ArrayList<Object> parameters = new ArrayList<Object>(length);
      parameters.add(message);
      for (int i = 1; i < length; ++i) {
        if (parameterTypes[i] == Envelop.class) {
          parameters.add(envelop);

        } else if (parameterTypes[i] == Agent.class) {
          parameters.add(agent);
        }
      }
      args = parameters.toArray();

    } else {
      args = new Object[]{message};
    }
    final Object result = method.invoke(object, args);
    handleReturnValue(method, result, envelop, agent);
  }
}
