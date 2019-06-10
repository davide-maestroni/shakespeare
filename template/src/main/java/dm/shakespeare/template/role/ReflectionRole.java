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

package dm.shakespeare.template.role;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;

import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Headers;
import dm.shakespeare.actor.SerializableRole;
import dm.shakespeare.template.actor.Behaviors;
import dm.shakespeare.template.actor.ReflectionBehavior;
import dm.shakespeare.template.config.BuildConfig;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 01/17/2019.
 */
public class ReflectionRole extends SerializableRole {

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

  private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

  private final Object object;

  public ReflectionRole() {
    object = this;
  }

  public ReflectionRole(@NotNull final Object object) {
    this.object = ConstantConditions.notNull("object", object);
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public static <T> T tellAs(@NotNull final Class<? super T> type, @NotNull final Actor actor,
      @Nullable final Headers headers, @NotNull final Actor sender) {
    return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
        new ActorHandler(actor, headers, sender));
  }

  @NotNull
  protected Behavior getSerializableBehavior(@NotNull final String id) {
    return Behaviors.reflection(object);
  }

  private static class ActorHandler implements InvocationHandler {

    private final Actor actor;
    private final Headers headers;
    private final Actor sender;

    private ActorHandler(@NotNull final Actor actor, @Nullable final Headers headers,
        @NotNull final Actor sender) {
      this.actor = ConstantConditions.notNull("actor", actor);
      this.sender = ConstantConditions.notNull("sender", sender);
      this.headers = headers;
    }

    public Object invoke(final Object o, final Method method, final Object[] objects) {
      actor.tell(ReflectionBehavior.invoke(method.getName(), method.getParameterTypes(), objects),
          headers, sender);
      return DEFAULT_RETURN_VALUES.get(method.getReturnType());
    }
  }
}
