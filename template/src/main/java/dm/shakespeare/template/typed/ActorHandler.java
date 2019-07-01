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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
import dm.shakespeare.template.typed.annotation.ActorFrom;
import dm.shakespeare.template.typed.annotation.HeadersFrom;
import dm.shakespeare.template.typed.message.InvocationException;
import dm.shakespeare.template.typed.message.InvocationResponse;
import dm.shakespeare.template.typed.message.InvocationResult;
import dm.shakespeare.util.ConstantConditions;
import dm.shakespeare.util.WeakIdentityHashMap;
import dm.shakespeare.util.WeakValueHashMap;

/**
 * Invocation handler class used to create a proxy handling invocation messages to and from a target
 * actor.
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
      Collections.synchronizedMap(new WeakIdentityHashMap<Object, ActorHandler>());

  private final Actor actor;
  private final Actor invocationActor;
  private final Script script;
  private final Class<?> type;

  private ActorHandler(@NotNull final Actor actor, @NotNull final Class<?> type,
      @NotNull final Script script) {
    this.actor = ConstantConditions.notNull("actor", actor);
    this.type = ConstantConditions.notNull("type", type);
    this.script = ConstantConditions.notNull("script", script);
    invocationActor = Stage.newActor(new InvocationRole());
  }

  @NotNull
  @SuppressWarnings("unchecked")
  static <T> T createProxy(@NotNull final Actor actor, @NotNull final Class<?> type,
      @NotNull final Script script) {
    final ActorHandler handler = new ActorHandler(actor, type, script);
    final Object proxy =
        Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    actorHandlers.put(proxy, handler);
    return (T) proxy;
  }

  @NotNull
  static Actor getActor(@NotNull final Object actor) {
    final ActorHandler actorHandler = actorHandlers.get(ConstantConditions.notNull("actor", actor));
    if (actorHandler == null) {
      throw new IllegalArgumentException("the specified object is not a typed actor");
    }
    return actorHandler.getActor();
  }

  public Object invoke(final Object proxy, final Method method, final Object[] objects) throws
      Throwable {
    // validate annotations
    int actorFromIndex = -1;
    int headersFromIndex = -1;
    final Class<?>[] parameterTypes = method.getParameterTypes();
    final ArrayList<Class<?>> paramClasses = new ArrayList<Class<?>>();
    final Annotation[][] parameterAnnotations = method.getParameterAnnotations();
    for (int i = 0; i < parameterAnnotations.length; ++i) {
      boolean isFrom = false;
      final Annotation[] annotations = parameterAnnotations[i];
      for (final Annotation annotation : annotations) {
        final Class<? extends Annotation> type = annotation.annotationType();
        if (type == ActorFrom.class) {
          if ((actorFromIndex >= 0) || !Actor.class.isAssignableFrom(parameterTypes[i])) {
            throw new UnsupportedOperationException(
                "a typed actor method cannot have more than one parameter annotated with "
                    + ActorFrom.class.getSimpleName() + " annotation");
          }
          actorFromIndex = i;
          isFrom = true;

        } else if (type == HeadersFrom.class) {
          if ((headersFromIndex >= 0) || !Headers.class.isAssignableFrom(parameterTypes[i])) {
            throw new UnsupportedOperationException(
                "a typed actor method cannot have more than one parameter annotated with "
                    + HeadersFrom.class.getSimpleName() + " annotation");
          }
          headersFromIndex = i;
          isFrom = true;
        }
      }
      if (!isFrom) {
        paramClasses.add(parameterTypes[i]);
      }
    }
    Actor actorFrom = null;
    Headers headersFrom = null;
    final ArrayList<Object> arguments = new ArrayList<Object>();
    final ArrayList<Actor> actors = new ArrayList<Actor>();
    final int length = (objects != null) ? objects.length : 0;
    for (int i = 0; i < length; ++i) {
      final Object object = objects[i];
      if (actorFromIndex == i) {
        actorFrom = (Actor) object;
        continue;

      } else if (headersFromIndex == i) {
        headersFrom = (Headers) object;
        continue;
      }

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
    final Long timeoutMillis = script.getResultTimeoutMillis(actor.getId(), method);
    if ((timeoutMillis != null) && (actorFromIndex >= 0)) {
      throw new UnsupportedOperationException(
          "methods with annotated sender actor cannot have a timeout");
    }
    final Actor actor = this.actor;
    final Headers headers = (headersFrom != null) ? headersFrom : Headers.EMPTY;
    final InvocationId invocationId = new InvocationId(UUID.randomUUID().toString());
    for (final Actor sender : actors) {
      actor.tell(invocationId, Headers.EMPTY, sender);
    }
    final Invocation invocation =
        new Invocation(invocationId.getId(), method.getName(), paramClasses.toArray(EMPTY_CLASSES),
            arguments.toArray());
    if (timeoutMillis != null) {
      final Actor invocationActor = this.invocationActor;
      final InvocationLatch latch = new InvocationLatch(invocationId.getId());
      invocationActor.tell(latch, Headers.EMPTY, actor);
      actor.tell(invocation, headers.withReceiptId(invocationId.getId()), invocationActor);
      return latch.awaitResult(timeoutMillis, TimeUnit.MILLISECONDS);
    }
    final Actor sender = (actorFrom != null) ? actorFrom : Stage.STAND_IN;
    actor.tell(invocation, headers, sender);
    return DEFAULT_RETURN_VALUES.get(method.getReturnType());
  }

  @NotNull
  Actor getActor() {
    return actor;
  }

  private static class InvocationBehavior extends AbstractBehavior {

    private final WeakValueHashMap<String, InvocationLatch> latches =
        new WeakValueHashMap<String, InvocationLatch>();

    public void onMessage(final Object message, @NotNull final Envelop envelop,
        @NotNull final Agent agent) {
      if (message instanceof InvocationLatch) {
        final InvocationLatch latch = (InvocationLatch) message;
        latches.put(latch.getInvocationId(), latch);

      } else if (message instanceof InvocationResponse) {
        final InvocationLatch latch =
            latches.remove(((InvocationResponse) message).getInvocationId());
        if (latch != null) {
          if (message instanceof InvocationResult) {
            latch.setResult(((InvocationResult) message).getResult());

          } else if (message instanceof InvocationException) {
            latch.setException(((InvocationException) message).getCause());
          }
        }

      } else if (message instanceof Bounce) {
        final InvocationLatch latch =
            latches.remove(((Bounce) message).getHeaders().getReceiptId());
        if (latch != null) {
          if (message instanceof Failure) {
            latch.setException(((Failure) message).getCause());

          } else {
            latch.setException(new IllegalStateException("typed actor is unreachable"));
          }
        }
      }
    }
  }

  private static class InvocationLatch extends CountDownLatch {

    private final String invocationId;

    private volatile Throwable exception;
    private volatile Object result;

    private InvocationLatch(@NotNull final String invocationId) {
      super(1);
      this.invocationId = invocationId;
    }

    Object awaitResult(final long timeout, @NotNull final TimeUnit unit) throws Throwable {
      if (await(timeout, unit)) {
        if (exception != null) {
          throw exception;
        }
        return result;
      }
      throw new InvocationTimeoutException(
          "invocation result not available after: " + timeout + " " + unit);
    }

    String getInvocationId() {
      return invocationId;
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
