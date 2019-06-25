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

package dm.shakespeare.template.typed;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import dm.shakespeare.Stage;
import dm.shakespeare.actor.AbstractBehavior;
import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.actor.Headers;
import dm.shakespeare.actor.Role;
import dm.shakespeare.message.Bounce;
import dm.shakespeare.message.Failure;
import dm.shakespeare.template.typed.TypedRole.TypedRoleSignal;
import dm.shakespeare.template.typed.actor.Script;
import dm.shakespeare.template.typed.annotation.FromActor;
import dm.shakespeare.template.typed.annotation.FromHeaders;
import dm.shakespeare.template.typed.message.InvocationException;
import dm.shakespeare.template.typed.message.InvocationResponse;
import dm.shakespeare.template.typed.message.InvocationResult;
import dm.shakespeare.util.ConstantConditions;
import dm.shakespeare.util.WeakValueHashMap;

/**
 * Created by davide-maestroni on 06/17/2019.
 */
class ActorHandler implements InvocationHandler {

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
  private static final Class<?>[] EMPTY_CLASSES = new Class<?>[0];

  private static final Map<Object, ActorHandler> actorHandlers =
      Collections.synchronizedMap(new WeakHashMap<Object, ActorHandler>());

  private final Actor actor;
  private final Script script;
  private final Actor invocationActor;
  private final Class<?> type;

  private ActorHandler(@NotNull final Class<?> type, @NotNull final Script script,
      @NotNull final Actor actor) {
    this.type = ConstantConditions.notNull("type", type);
    this.script = ConstantConditions.notNull("script", script);
    this.actor = ConstantConditions.notNull("actor", actor);
    invocationActor = Stage.newActor(new InvocationRole());
  }

  @NotNull
  @SuppressWarnings("unchecked")
  static <T> T createProxy(@NotNull final Class<?> type, @NotNull final Script script,
      @NotNull final Actor actor) {
    final ActorHandler handler = new ActorHandler(type, script, actor);
    final Object proxy =
        Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    actorHandlers.put(proxy, handler);
    return (T) proxy;
  }

  public Object invoke(final Object o, final Method method, final Object[] objects) throws
      Throwable {
    // validate annotations
    int fromActor = -1;
    int fromHeaders = -1;
    final Class<?>[] parameterTypes = method.getParameterTypes();
    final ArrayList<Class<?>> paramClasses = new ArrayList<Class<?>>();
    final Annotation[][] parameterAnnotations = method.getParameterAnnotations();
    for (int i = 0; i < parameterAnnotations.length; ++i) {
      boolean isFrom = false;
      final Annotation[] annotations = parameterAnnotations[i];
      for (final Annotation annotation : annotations) {
        final Class<? extends Annotation> type = annotation.annotationType();
        if (type == FromActor.class) {
          if ((fromActor >= 0) || !Actor.class.isAssignableFrom(parameterTypes[i])) {
            throw new IllegalStateException();
          }
          fromActor = i;
          isFrom = true;

        } else if (type == FromHeaders.class) {
          if ((fromHeaders >= 0) || !Headers.class.isAssignableFrom(parameterTypes[i])) {
            throw new IllegalStateException();
          }
          fromHeaders = i;
          isFrom = true;
        }
      }
      if (!isFrom) {
        paramClasses.add(parameterTypes[i]);
      }
    }
    final ArrayList<Object> arguments = new ArrayList<Object>();
    final ArrayList<Actor> actors = new ArrayList<Actor>();
    for (int i = 0; i < objects.length; ++i) {
      if ((fromActor == i) || (fromHeaders == i)) {
        continue;
      }

      final Object object = objects[i];
      if (object != null) {
        if (List.class.isAssignableFrom(object.getClass())) {
          final ArrayList<Object> list = new ArrayList<Object>();
          for (final Object element : (List<?>) object) {
            final ActorHandler handler = actorHandlers.get(element);
            if (handler != null) {
              list.add(new InvocationArg(handler.type, handler.script));
              actors.add(handler.actor);

            } else if (element instanceof Actor) {
              list.add(TypedRoleSignal.ACTOR_ARG);
              actors.add((Actor) element);

            } else {
              list.add(element);
            }
          }
          arguments.add(list);

        } else {
          final ActorHandler handler = actorHandlers.get(object);
          if (handler != null) {
            arguments.add(new InvocationArg(handler.type, handler.script));
            actors.add(handler.actor);

          } else if (object instanceof Actor) {
            arguments.add(TypedRoleSignal.ACTOR_ARG);
            actors.add((Actor) object);

          } else {
            arguments.add(object);
          }
        }

      } else {
        arguments.add(null);
      }
    }
    final Long timeoutMillis = script.getTimeoutMillis(actor.getId(), method);
    if ((timeoutMillis != null) && ((fromActor >= 0) || (fromHeaders >= 0))) {
      throw new IllegalStateException();
    }
    final Actor actor = this.actor;
    final Headers headers = (fromHeaders >= 0) ? (Headers) objects[fromHeaders] : Headers.NONE;
    final InvocationId invocationId = new InvocationId(UUID.randomUUID().toString());
    for (final Actor sender : actors) {
      actor.tell(invocationId, Headers.NONE, sender);
    }
    final Invocation invocation =
        new Invocation(invocationId.getId(), method.getName(), paramClasses.toArray(EMPTY_CLASSES),
            arguments.toArray());
    if (timeoutMillis != null) {
      final Actor invocationActor = this.invocationActor;
      final InvocationLatch latch = new InvocationLatch();
      final Headers invocationHeaders =
          headers.withThreadId(invocationId.getId()).withReceiptId(invocationId.getId());
      invocationActor.tell(latch, invocationHeaders, actor);
      actor.tell(invocation, invocationHeaders, invocationActor);
      return latch.awaitResult(timeoutMillis, TimeUnit.MILLISECONDS);
    }
    final Actor sender = (fromActor >= 0) ? (Actor) objects[fromActor] : Stage.STAND_IN;
    actor.tell(invocation, headers, sender);
    return DEFAULT_RETURN_VALUES.get(method.getReturnType());
  }

  private static class InvocationBehavior extends AbstractBehavior {

    private final WeakValueHashMap<String, InvocationLatch> latches =
        new WeakValueHashMap<String, InvocationLatch>();

    public void onMessage(final Object message, @NotNull final Envelop envelop,
        @NotNull final Agent agent) {
      if (message instanceof InvocationLatch) {
        latches.put(envelop.getHeaders().getThreadId(), (InvocationLatch) message);

      } else if (message instanceof InvocationResponse) {
        final InvocationLatch latch = latches.remove(envelop.getHeaders().getThreadId());
        if (latch != null) {
          if (message instanceof InvocationResult) {
            latch.setResult(((InvocationResult) message).getResult());

          } else if (message instanceof InvocationException) {
            latch.setException(((InvocationException) message).getException());
          }
        }

      } else if (message instanceof Bounce) {
        final InvocationLatch latch = latches.remove(envelop.getHeaders().getThreadId());
        if (latch != null) {
          if (message instanceof Failure) {
            latch.setException(((Failure) message).getCause());

          } else {
            latch.setException(new IllegalStateException());
          }
        }
      }
    }
  }

  private static class InvocationLatch extends CountDownLatch {

    private volatile Throwable exception;
    private volatile Object result;

    private InvocationLatch() {
      super(1);
    }

    Object awaitResult(final long timeout, @NotNull final TimeUnit unit) throws Throwable {
      if (await(timeout, unit)) {
        if (exception != null) {
          throw exception;
        }
        return result;
      }
      throw new TimeoutException();
    }

    void setException(final Throwable exception) {
      this.exception = exception;
      countDown();
    }

    void setResult(final Object result) {
      this.result = result;
      countDown();
    }
  }

  private static class InvocationRole extends Role {

    @NotNull
    public Behavior getBehavior(@NotNull final String id) {
      return new InvocationBehavior();
    }
  }
}
