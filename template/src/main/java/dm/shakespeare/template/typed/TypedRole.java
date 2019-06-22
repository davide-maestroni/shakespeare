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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.actor.Headers;
import dm.shakespeare.actor.SerializableAbstractBehavior;
import dm.shakespeare.actor.SerializableRole;
import dm.shakespeare.log.Logger;
import dm.shakespeare.template.config.BuildConfig;
import dm.shakespeare.template.typed.actor.Script;
import dm.shakespeare.template.typed.message.InvocationException;
import dm.shakespeare.template.typed.message.InvocationResult;
import dm.shakespeare.template.util.Reflections;
import dm.shakespeare.util.CQueue;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 01/17/2019.
 */
class TypedRole extends SerializableRole {

  private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

  private final Script script;
  private final Object object;
  private final Object[] objectArgs;
  private final Class<?> objectClass;

  TypedRole(@NotNull final Script script, @NotNull final Object object) {
    this.script = ConstantConditions.notNull("script", script);
    this.object = ConstantConditions.notNull("object", object);
    this.objectClass = object.getClass();
    this.objectArgs = null;
  }

  TypedRole(@NotNull final Script script, @NotNull final Class<?> type,
      @NotNull final Object... args) {
    this.script = ConstantConditions.notNull("script", script);
    this.object = null;
    this.objectClass = ConstantConditions.notNull("type", type);
    this.objectArgs = ConstantConditions.notNull("args", args);
  }

  @NotNull
  @Override
  public ExecutorService getExecutorService(@NotNull final String id) throws Exception {
    return script.getExecutorService(id);
  }

  @NotNull
  @Override
  public Logger getLogger(@NotNull final String id) throws Exception {
    return script.getLogger(id);
  }

  @Override
  public int getQuota(@NotNull final String id) throws Exception {
    return script.getQuota(id);
  }

  @NotNull
  protected Behavior getSerializableBehavior(@NotNull final String id) {
    return new TypedBehavior(getInstance());
  }

  @NotNull
  private Object getInstance() {
    final Object object = this.object;
    if (object != null) {
      return object;
    }

    return Reflections.newInstance(this.objectClass, this.objectArgs);
  }

  enum TypedRoleSignal {
    ACTOR_ARG
  }

  private static class ActorArg {

    private final CQueue<Actor> actors = new CQueue<Actor>();

    private long timeoutMillis;
  }

  private static class TypedBehavior extends SerializableAbstractBehavior {

    private static final long EXPIRATION_MILLIS = TimeUnit.MINUTES.toMillis(15);

    private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

    private final HashMap<String, ActorArg> actorArgs = new HashMap<String, ActorArg>();
    private final Object instance;

    private TypedBehavior(@NotNull final Object instance) {
      this.instance = instance;
    }

    public void onMessage(final Object message, @NotNull final Envelop envelop,
        @NotNull final Agent agent) {
      if (message instanceof InvocationId) {
        final String invocationId = ((InvocationId) message).getId();
        final HashMap<String, ActorArg> actorArgs = this.actorArgs;
        ActorArg actorArg = actorArgs.get(invocationId);
        if (actorArg == null) {
          actorArg = new ActorArg();
          actorArgs.put(invocationId, actorArg);
        }
        final long now = System.currentTimeMillis();
        actorArg.actors.add(envelop.getSender());
        actorArg.timeoutMillis = now + EXPIRATION_MILLIS;
        final Iterator<ActorArg> iterator = actorArgs.values().iterator();
        while (iterator.hasNext()) {
          if (iterator.next().timeoutMillis < now) {
            iterator.remove();
          }
        }

      } else if ((message != null) && (message.getClass() == Invocation.class)) {
        final Object instance = this.instance;
        final Invocation invocation = (Invocation) message;
        final Method method;
        final Class<?>[] parameterTypes = invocation.getParameterTypes();
        try {
          method = Reflections.makeAccessible(
              instance.getClass().getMethod(invocation.getMethodName(), parameterTypes));

        } catch (final NoSuchMethodException e) {
          agent.getLogger().wrn(e, "ignoring message: %s", message);
          envelop.getSender()
              .tell(new InvocationException(e), envelop.getHeaders().threadOnly(), agent.getSelf());
          return;
        }

        final Actor self = agent.getSelf();
        final Headers headers = envelop.getHeaders().threadOnly();
        try {
          final ActorArg actorArg = actorArgs.remove(invocation.getId());
          final Object[] args;
          final Object[] arguments = invocation.getArguments();
          if (actorArg != null) {
            args = new Object[parameterTypes.length];
            for (int i = 0; i < arguments.length; ++i) {
              final Object argument = arguments[i];
              if (argument instanceof InvocationArg) {
                final InvocationArg invocationArg = (InvocationArg) argument;
                final Class<?> type = invocationArg.getType();
                args[i] = ActorHandler.createProxy(type, invocationArg.getScript(),
                    actorArg.actors.removeFirst());

              } else if (argument == TypedRoleSignal.ACTOR_ARG) {
                args[i] = actorArg.actors.removeFirst();

              } else if (argument instanceof List) {
                final ArrayList<Object> list = new ArrayList<Object>();
                for (final Object element : (List<?>) argument) {
                  if (element instanceof InvocationArg) {
                    final InvocationArg invocationArg = (InvocationArg) element;
                    final Class<?> type = invocationArg.getType();
                    list.add(ActorHandler.createProxy(type, invocationArg.getScript(),
                        actorArg.actors.removeFirst()));

                  } else if (element == TypedRoleSignal.ACTOR_ARG) {
                    list.add(actorArg.actors.removeFirst());

                  } else {
                    list.add(element);
                  }
                }
                args[i] = list;

              } else {
                args[i] = argument;
              }
            }

          } else {
            args = arguments;
          }
          final Object result = method.invoke(instance, args);
          envelop.getSender().tell(new InvocationResult(result), headers, self);

        } catch (final Throwable t) {
          if (t instanceof InvocationTargetException) {
            final Throwable exception = ((InvocationTargetException) t).getTargetException();
            envelop.getSender().tell(new InvocationException(exception), headers, self);
            if (exception instanceof InterruptedException) {
              Thread.currentThread().interrupt();

            } else if (exception instanceof Error) {
              // rethrow errors
              throw (Error) exception;
            }

          } else {
            envelop.getSender().tell(new InvocationException(t), headers, self);
            if (t instanceof Error) {
              // rethrow errors
              throw (Error) t;
            }
          }
        }

      } else {
        agent.getLogger().wrn("ignoring message: %s", message);
      }
    }
  }
}
