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

package dm.shakespeare.template.script;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

import dm.shakespeare.actor.Behavior.Context;
import dm.shakespeare.actor.BehaviorBuilder;
import dm.shakespeare.template.annotation.OnStart;

/**
 * Created by davide-maestroni on 09/07/2018.
 */
class OnStartHandler implements AnnotationHandler<OnStart> {

  @SuppressWarnings("unchecked")
  public void handle(@NotNull final BehaviorBuilder builder, @NotNull final Object object,
      @NotNull final Method method, @NotNull final OnStart annotation) {
    final String name = method.getName();
    final Class<?>[] parameterTypes = method.getParameterTypes();
    if (parameterTypes.length > 0) {
      for (final Class<?> parameterType : parameterTypes) {
        if (parameterType != Context.class) {
          throw new IllegalArgumentException("invalid method parameters: " + name);
        }
      }
    }
    builder.onStart(new MethodObserver(object, method));
  }
}
