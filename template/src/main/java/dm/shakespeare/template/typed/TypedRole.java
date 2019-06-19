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
import java.util.List;
import java.util.concurrent.ExecutorService;

import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.actor.Headers;
import dm.shakespeare.actor.SerializableAbstractBehavior;
import dm.shakespeare.actor.SerializableRole;
import dm.shakespeare.log.Logger;
import dm.shakespeare.template.config.BuildConfig;
import dm.shakespeare.template.typed.background.Background;
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

  private final Background background;
  private final Object object;
  private final Object[] objectArgs;
  private final Class<?> objectClass;

  TypedRole(@NotNull final Background background, @NotNull final Object object) {
    this.background = ConstantConditions.notNull("background", background);
    this.object = ConstantConditions.notNull("object", object);
    this.objectClass = object.getClass();
    this.objectArgs = null;
  }

  TypedRole(@NotNull final Background background, @NotNull final Class<?> type,
      @NotNull final Object... args) {
    this.background = ConstantConditions.notNull("background", background);
    this.object = null;
    this.objectClass = ConstantConditions.notNull("type", type);
    this.objectArgs = ConstantConditions.notNull("args", args);
  }

  @NotNull
  @Override
  public ExecutorService getExecutorService(@NotNull final String id) throws Exception {
    return background.getExecutorService(id);
  }

  @NotNull
  @Override
  public Logger getLogger(@NotNull final String id) throws Exception {
    return background.getLogger(id);
  }

  @Override
  public int getQuota(@NotNull final String id) throws Exception {
    return background.getQuota(id);
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
    TYPED_ARG, ACTOR_ARG
  }

  private static class TypedBehavior extends SerializableAbstractBehavior {

    private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

    private final Object instance;
    private final HashMap<String, CQueue<Actor>> typedArgs = new HashMap<String, CQueue<Actor>>();

    private TypedBehavior(@NotNull final Object instance) {
      this.instance = instance;
    }

    public void onMessage(final Object message, @NotNull final Envelop envelop,
        @NotNull final Agent agent) {
      if (message == TypedRoleSignal.TYPED_ARG) {
        final String threadId = envelop.getHeaders().getThreadId();
        final HashMap<String, CQueue<Actor>> typedArgs = this.typedArgs;
        CQueue<Actor> actors = typedArgs.get(threadId);
        if (actors == null) {
          actors = new CQueue<Actor>();
          typedArgs.put(threadId, actors);
        }
        actors.add(envelop.getSender());

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
          final CQueue<Actor> actors = typedArgs.remove(envelop.getHeaders().getThreadId());
          final Object[] args;
          final Object[] arguments = invocation.getArguments();
          if (actors != null) {
            args = new Object[parameterTypes.length];
            for (int i = 0; i < arguments.length; ++i) {
              final Object argument = arguments[i];
              if (argument instanceof InvocationArg) {
                final InvocationArg invocationArg = (InvocationArg) argument;
                final Class<?> type = invocationArg.getType();
                args[i] = ActorHandler.createProxy(type, invocationArg.getBackground(),
                    actors.removeFirst());

              } else if (argument == TypedRoleSignal.ACTOR_ARG) {
                args[i] = actors.removeFirst();

              } else if (argument instanceof List) {
                final ArrayList<Object> list = new ArrayList<Object>();
                for (final Object element : (List<?>) argument) {
                  if (element instanceof InvocationArg) {
                    final InvocationArg invocationArg = (InvocationArg) element;
                    final Class<?> type = invocationArg.getType();
                    list.add(ActorHandler.createProxy(type, invocationArg.getBackground(),
                        actors.removeFirst()));

                  } else if (element == TypedRoleSignal.ACTOR_ARG) {
                    list.add(actors.removeFirst());

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
          // TODO: 2019-06-17 @ReplyTo
          envelop.getSender().tell(new InvocationResult(result), headers, self);

        } catch (final Throwable t) {
          if (t instanceof InvocationTargetException) {
            final Throwable exception = ((InvocationTargetException) t).getTargetException();
            // TODO: 2019-06-17 @ReplyTo
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
