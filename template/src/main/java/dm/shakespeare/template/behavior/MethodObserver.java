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
import java.util.Collections;

import dm.shakespeare.actor.Behavior.Agent;
import dm.shakespeare.function.Observer;
import dm.shakespeare.template.config.BuildConfig;
import dm.shakespeare.util.ConstantConditions;

/**
 * {@code Observer} calling an annotated method of the target object on a start or stop event.
 */
class MethodObserver implements Observer<Agent>, Serializable {

  private static final Object[] NO_ARGS = new Object[0];

  private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

  private final Method method;
  private final Object object;

  MethodObserver(@NotNull final Object object, @NotNull final Method method) {
    this.object = ConstantConditions.notNull("object", object);
    this.method = ConstantConditions.notNull("method", method);
  }

  private MethodObserver() {
    object = null;
    method = null;
  }

  public void accept(final Agent agent) throws Exception {
    final Method method = this.method;
    final Class<?>[] parameterTypes = method.getParameterTypes();
    final int length = parameterTypes.length;
    final Object[] args;
    if (length > 0) {
      if (length == 1) {
        args = new Object[]{agent};

      } else {
        args = Collections.nCopies(length, agent).toArray();
      }

    } else {
      args = NO_ARGS;
    }
    agent.getLogger()
        .dbg("[%s] invoking method: method=%s - args=%s", agent.getSelf(), method, args);
    method.invoke(object, args);
  }

  public Method getMethod() {
    return method;
  }

  public Object getObject() {
    return object;
  }
}
