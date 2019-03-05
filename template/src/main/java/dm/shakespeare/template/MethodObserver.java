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
import java.util.Collections;

import dm.shakespeare.actor.Behavior.Context;
import dm.shakespeare.function.Observer;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 09/12/2018.
 */
class MethodObserver implements Observer<Context> {

  private static final Object[] EMPTY_ARGS = new Object[0];

  private final Method mMethod;
  private final Object mObject;

  MethodObserver(@NotNull final Object object, @NotNull final Method method) {
    mObject = ConstantConditions.notNull("object", object);
    mMethod = Methods.makeAccessible(method);
  }

  public void accept(final Context context) throws Exception {
    final Method method = mMethod;
    final Class<?>[] parameterTypes = method.getParameterTypes();
    final int length = parameterTypes.length;
    final Object[] args;
    if (length > 0) {
      if (length == 1) {
        args = new Object[]{context};

      } else {
        args = Collections.nCopies(length, context).toArray();
      }

    } else {
      args = EMPTY_ARGS;
    }
    method.invoke(mObject, args);
  }
}
