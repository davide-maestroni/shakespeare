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
import dm.shakespeare.util.CQueue;
import dm.shakespeare.util.ConstantConditions;

/**
 * {@code Role} implementation defining the behavior of a typed actor.
 */
class TypedRole extends SerializableRole {

  private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

  private final Script script;

  TypedRole(@NotNull final Script script) {
    this.script = ConstantConditions.notNull("script", script);
  }

  private TypedRole() {
    script = null;
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

  public Script getScript() {
    return script;
  }

  @NotNull
  protected Behavior getSerializableBehavior(@NotNull final String id) throws Exception {
    return new TypedBehavior(script.getRole(id));
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
    private final Object role;

    private TypedBehavior() {
      role = this;
    }

    private TypedBehavior(@NotNull final Object role) {
      this.role = ConstantConditions.notNull("role", role);
    }

    public HashMap<String, ActorArg> getActorArgs() {
      return actorArgs;
    }

    public Object getRole() {
      return role;
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
        final Actor self = agent.getSelf();
        agent.getLogger()
            .dbg("[%s] handling invocation message: envelop=%s - message=%s", self, envelop,
                message);
        final Object instance = this.role;
        final Invocation invocation = (Invocation) message;
        final Headers headers = envelop.getHeaders().threadOnly();
        final Method method;
        final Class<?>[] parameterTypes = invocation.getParameterTypes();
        try {
          method = instance.getClass().getMethod(invocation.getMethodName(), parameterTypes);

        } catch (final NoSuchMethodException e) {
          agent.getLogger()
              .err(e, "[%s] invocation failure, no method found: envelop=%s - message=%s", self,
                  envelop, message);
          envelop.getSender()
              .tell(new InvocationException(invocation.getId(), new InvocationMismatchException(e)),
                  headers, self);
          return;
        }

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
                args[i] = ActorHandler.createProxy(actorArg.actors.removeFirst(), type,
                    invocationArg.getScript());

              } else if (argument == TypedRoleSignal.ACTOR_ARG) {
                args[i] = actorArg.actors.removeFirst();

              } else if (argument instanceof List) {
                final ArrayList<Object> list = new ArrayList<Object>();
                for (final Object element : (List<?>) argument) {
                  if (element instanceof InvocationArg) {
                    final InvocationArg invocationArg = (InvocationArg) element;
                    final Class<?> type = invocationArg.getType();
                    list.add(ActorHandler.createProxy(actorArg.actors.removeFirst(), type,
                        invocationArg.getScript()));

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
          envelop.getSender().tell(new InvocationResult(invocation.getId(), result), headers, self);

        } catch (final Throwable t) {
          agent.getLogger()
              .err(t, "[%s] invocation failure: envelop=%s - message=%s", self, envelop, message);
          if (t instanceof InvocationTargetException) {
            final Throwable exception = ((InvocationTargetException) t).getTargetException();
            envelop.getSender()
                .tell(new InvocationException(invocation.getId(), exception), headers, self);
            if (exception instanceof InterruptedException) {
              Thread.currentThread().interrupt();

            } else if (exception instanceof Error) {
              // rethrow errors
              throw (Error) exception;
            }

          } else {
            envelop.getSender().tell(new InvocationException(invocation.getId(), t), headers, self);
            if (t instanceof Error) {
              // rethrow errors
              throw (Error) t;
            }
          }
        }

      } else {
        agent.getLogger()
            .wrn("[%s] ignoring message: envelop=%s - message=%s", agent.getSelf(), envelop,
                message);
      }
    }
  }
}
