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

import dm.shakespeare.actor.Behavior.Agent;
import dm.shakespeare.actor.BehaviorBuilder;
import dm.shakespeare.actor.BehaviorBuilder.Matcher;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.template.annotation.OnMatch;
import dm.shakespeare.template.annotation.VoidMatcher;
import dm.shakespeare.template.config.BuildConfig;
import dm.shakespeare.template.util.Reflections;

/**
 * Created by davide-maestroni on 09/07/2018.
 */
class OnMatchHandler implements AnnotationHandler<OnMatch> {

  private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

  @SuppressWarnings("unchecked")
  public void handle(@NotNull final BehaviorBuilder builder, @NotNull final Object object,
      @NotNull final Method method, @NotNull final OnMatch annotation) throws Exception {
    Matcher<?> matcher = null;
    final Class<? extends Matcher<?>> matcherClass = annotation.matcherClass();
    final String name = annotation.matcherName();
    if (matcherClass != VoidMatcher.class) {
      if (!name.isEmpty()) {
        throw new IllegalArgumentException(
            "only one of matcherClass and matcherName parameters must be specified");
      }
      matcher = matcherClass.newInstance();

    } else {
      if (name.isEmpty()) {
        throw new IllegalArgumentException(
            "at least one of matcherClass and matcherName parameters must be specified");
      }

      for (final Method matcherMethod : object.getClass().getMethods()) {
        final Class<?> returnType = matcherMethod.getReturnType();
        final Class<?>[] parameterTypes = matcherMethod.getParameterTypes();
        if (name.equals(matcherMethod.getName()) && ((returnType == Boolean.class) || (returnType
            == boolean.class)) && (parameterTypes.length == 3)
            && parameterTypes[1].isAssignableFrom(Envelop.class)
            && parameterTypes[2].isAssignableFrom(Agent.class)) {
          matcher = new MessageMatcher(object, matcherMethod);
          break;
        }
      }

      if (matcher == null) {
        throw new IllegalArgumentException("cannot find matcher method: " + name);
      }
    }
    final Class<?>[] parameterTypes = method.getParameterTypes();
    final int length = parameterTypes.length;
    if (length > 1) {
      for (int i = 1; i < length; ++i) {
        final Class<?> parameterType = parameterTypes[i];
        if ((parameterType != Envelop.class) && (parameterType != Agent.class)) {
          throw new IllegalArgumentException("invalid method parameters: " + name);
        }
      }
    }
    builder.onMatch((Matcher<? super Object>) matcher,
        new dm.shakespeare.template.behavior.MethodHandler(object, method));
  }

  private static class MessageMatcher implements Matcher<Object>, Serializable {

    private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

    private final Method method;
    private final Object object;

    private MessageMatcher() {
      object = null;
      method = null;
    }

    private MessageMatcher(@NotNull final Object object, @NotNull final Method method) {
      this.object = object;
      this.method = Reflections.makeAccessible(method);
    }

    // json
    @NotNull
    public Method getMethod() {
      return method;
    }

    // json
    @NotNull
    public Object getObject() {
      return object;
    }

    public boolean match(final Object message, @NotNull final Envelop envelop,
        @NotNull final Agent agent) throws Exception {
      return (Boolean) method.invoke(object, message, envelop, agent);
    }
  }
}
