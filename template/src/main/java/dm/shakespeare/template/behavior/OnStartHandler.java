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

import java.lang.reflect.Method;

import dm.shakespeare.actor.Behavior.Agent;
import dm.shakespeare.actor.BehaviorBuilder;
import dm.shakespeare.template.behavior.annotation.OnStart;
import dm.shakespeare.template.config.BuildConfig;

/**
 * {@code AnnotationHandler} handling {@link OnStart} annotations.
 */
class OnStartHandler implements AnnotationHandler<OnStart> {

  private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

  public void handle(@NotNull final BehaviorBuilder builder, @NotNull final Object object,
      @NotNull final Method method, @NotNull final OnStart annotation) {
    final String name = method.getName();
    final Class<?>[] parameterTypes = method.getParameterTypes();
    if (parameterTypes.length > 0) {
      for (final Class<?> parameterType : parameterTypes) {
        if (parameterType != Agent.class) {
          throw new IllegalArgumentException("invalid method parameters: " + name);
        }
      }
    }
    builder.onStart(new MethodObserver(object, method));
  }
}
