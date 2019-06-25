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

package dm.shakespeare;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;

import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Behavior.Agent;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.actor.Headers;
import dm.shakespeare.concurrent.ActorExecutorService;
import dm.shakespeare.concurrent.ExecutorServices;
import dm.shakespeare.log.Logger;
import dm.shakespeare.message.Bounce;
import dm.shakespeare.message.DeadLetter;
import dm.shakespeare.message.Delivery;
import dm.shakespeare.message.Failure;
import dm.shakespeare.util.ConstantConditions;

/**
 * Class implementing a behavior agent.
 */
class StandardAgent implements Agent {

  private static final DeadLetter DEAD_LETTER = new DeadLetter();

  private final ActorExecutorService actorExecutorService;
  private final Logger logger;
  private final HashSet<Actor> observers = new HashSet<Actor>();

  private Actor actor;
  private AgentExecutorService agentExecutorService;
  private AgentScheduledExecutorService agentScheduledExecutorService;
  private Behavior behavior;
  private BehaviorWrapper behaviorWrapper = new BehaviorStarter();
  private Runnable dismissRunnable;
  private volatile boolean dismissed;
  private Runnable restartRunnable;
  private volatile Thread runner;
  private boolean stopped;

  /**
   * Creates a new agent instance.
   *
   * @param behavior        the initial behavior instance
   * @param executorService the backing executor service.
   * @param logger          the logger instance.
   */
  StandardAgent(@NotNull final Behavior behavior, @NotNull final ExecutorService executorService,
      @NotNull final Logger logger) {
    this.behavior = ConstantConditions.notNull("behavior", behavior);
    actorExecutorService =
        (executorService instanceof ScheduledExecutorService) ? ExecutorServices.asActorExecutor(
            (ScheduledExecutorService) executorService)
            : ExecutorServices.asActorExecutor(executorService);
    this.logger = ConstantConditions.notNull("logger", logger);
  }

  @NotNull
  public ExecutorService getExecutorService() {
    if (agentExecutorService == null) {
      final ActorExecutorService actorExecutorService = this.actorExecutorService;
      if (actorExecutorService instanceof ScheduledExecutorService) {
        agentExecutorService = (agentScheduledExecutorService =
            new AgentScheduledExecutorService((ScheduledExecutorService) actorExecutorService,
                this));

      } else {
        agentExecutorService = new AgentExecutorService(actorExecutorService, this);
      }
    }
    return agentExecutorService;
  }

  @NotNull
  public Logger getLogger() {
    return logger;
  }

  @NotNull
  public ScheduledExecutorService getScheduledExecutorService() {
    if (agentScheduledExecutorService == null) {
      final ActorExecutorService actorExecutorService = this.actorExecutorService;
      if (actorExecutorService instanceof ScheduledExecutorService) {
        agentExecutorService = (agentScheduledExecutorService =
            new AgentScheduledExecutorService((ScheduledExecutorService) actorExecutorService,
                this));

      } else {
        agentScheduledExecutorService = new AgentScheduledExecutorService(
            ExecutorServices.asScheduled(this.actorExecutorService), this);
      }
    }
    return agentScheduledExecutorService;
  }

  @NotNull
  public Actor getSelf() {
    return actor;
  }

  public boolean isDismissed() {
    return stopped || dismissed;
  }

  public void restartSelf() {
    logger.dbg("[%s] restarting self", actor);
    if (restartRunnable == null) {
      restartRunnable = new Runnable() {

        public void run() {
          final Thread runner = (StandardAgent.this.runner = Thread.currentThread());
          try {
            behaviorWrapper.onRestart(StandardAgent.this);

          } finally {
            StandardAgent.this.runner = null;
          }

          if (runner.isInterrupted()) {
            logger.wrn("[%s] thread has been interrupted!", actor);
            behaviorWrapper.onStop(StandardAgent.this);
          }
        }
      };
    }
    actorExecutorService.executeNext(restartRunnable);
  }

  public void setBehavior(@NotNull final Behavior behavior) {
    logger.dbg("[%s] setting new behavior: behavior=%s", actor, behavior);
    this.behavior = ConstantConditions.notNull("behavior", behavior);
  }

  void addObserver(@NotNull final Actor observer) {
    if (stopped) {
      observer.tell(DEAD_LETTER, Headers.NONE, actor);

    } else {
      observers.add(observer);
    }
  }

  void dismiss() {
    if (dismissRunnable == null) {
      dismissRunnable = new Runnable() {

        public void run() {
          behaviorWrapper.onStop(StandardAgent.this);
        }
      };
    }
    try {
      dismissed = true;
      actorExecutorService.executeNext(dismissRunnable);

    } catch (final RejectedExecutionException e) {
      dismissed = false;
      throw e;
    }
  }

  void dismissLazy() {
    if (dismissRunnable == null) {
      dismissRunnable = new Runnable() {

        public void run() {
          behaviorWrapper.onStop(StandardAgent.this);
        }
      };
    }
    actorExecutorService.execute(dismissRunnable);
  }

  void dismissNow() {
    final Thread runner = this.runner;
    if (runner != null) {
      runner.interrupt();
    }
    dismiss();
  }

  @NotNull
  ActorExecutorService getActorExecutorService() {
    return actorExecutorService;
  }

  void message(final Object message, @NotNull final Envelop envelop) {
    final Thread runner = (this.runner = Thread.currentThread());
    try {
      behaviorWrapper.onMessage(message, envelop, this);

    } finally {
      this.runner = null;
    }

    if (runner.isInterrupted()) {
      logger.wrn("[%s] thread has been interrupted!", actor);
      behaviorWrapper.onStop(this);
    }
  }

  void messages(@NotNull final Iterable<?> messages, @NotNull final Envelop envelop) {
    final Headers headers = envelop.getHeaders();
    if (isDismissed() && (headers.getReceiptId() != null)) {
      final ArrayList<Object> bounces = new ArrayList<Object>();
      for (final Object message : messages) {
        bounces.add(new Bounce(message, headers));
      }
      envelop.getSender().tellAll(bounces, headers.threadOnly(), actor);
      return;
    }
    final Thread runner = (this.runner = Thread.currentThread());
    try {
      for (final Object message : messages) {
        behaviorWrapper.onMessage(message, envelop, this);
      }

    } finally {
      this.runner = null;
    }

    if (runner.isInterrupted()) {
      logger.wrn("[%s] thread has been interrupted!", actor);
      behaviorWrapper.onStop(this);
    }
  }

  void removeObserver(@NotNull final Actor observer) {
    observers.remove(observer);
  }

  void setActor(@NotNull final Actor actor) {
    this.actor = ConstantConditions.notNull("actor", actor);
  }

  private void cancelTasks() {
    final AgentExecutorService executorService = agentExecutorService;
    if (executorService != null) {
      executorService.cancelAll(true);
    }
    final AgentScheduledExecutorService scheduledExecutorService = agentScheduledExecutorService;
    if (scheduledExecutorService != null) {
      scheduledExecutorService.cancelAll(true);
    }
  }

  private void setStopped() {
    stopped = true;
    final Actor actor = this.actor;
    for (final Actor observer : observers) {
      observer.tell(DEAD_LETTER, Headers.NONE, actor);
    }
  }

  private class BehaviorStarter extends BehaviorWrapper {

    @Override
    public void onMessage(final Object message, @NotNull final Envelop envelop,
        @NotNull final Agent agent) {
      behaviorWrapper = new BehaviorWrapper();
      super.onStart(agent);
      super.onMessage(message, envelop, agent);
    }

    @Override
    public void onStop(@NotNull final Agent agent) {
      setStopped();
    }
  }

  private class BehaviorWrapper implements Behavior {

    public void onMessage(final Object message, @NotNull final Envelop envelop,
        @NotNull final Agent agent) {
      final Actor actor = StandardAgent.this.actor;
      logger.dbg("[%s] handling new message: envelop=%s - message=%s", actor, envelop, message);
      if (isDismissed()) {
        final Headers headers = envelop.getHeaders();
        if (headers.getReceiptId() != null) {
          envelop.getSender().tell(new Bounce(message, headers), headers.threadOnly(), actor);
        }
        return;
      }
      final Headers headers = envelop.getHeaders();
      if (headers.getReceiptId() != null) {
        try {
          behavior.onMessage(message, envelop, agent);
          if (!envelop.isPreventReceipt()) {
            envelop.getSender().tell(new Delivery(message, headers), headers.threadOnly(), actor);
          }

        } catch (final Throwable t) {
          if (!envelop.isPreventReceipt()) {
            envelop.getSender().tell(new Failure(message, headers, t), headers.threadOnly(), actor);
          }
          onStop(agent);
          if (t instanceof InterruptedException) {
            Thread.currentThread().interrupt();

          } else if (t instanceof Error) {
            // rethrow errors
            throw (Error) t;
          }
        }

      } else {
        try {
          behavior.onMessage(message, envelop, agent);

        } catch (final Throwable t) {
          onStop(agent);
          if (t instanceof InterruptedException) {
            Thread.currentThread().interrupt();

          } else if (t instanceof Error) {
            // rethrow errors
            throw (Error) t;
          }
        }
      }
    }

    public void onStart(@NotNull final Agent agent) {
      logger.dbg("[%s] staring actor", actor);
      try {
        behavior.onStart(agent);

      } catch (final Throwable t) {
        setStopped();
        cancelTasks();
        logger.wrn(t, "[%s] ignoring exception", actor);
        if (t instanceof InterruptedException) {
          Thread.currentThread().interrupt();

        } else if (t instanceof Error) {
          // rethrow errors
          throw (Error) t;
        }
      }
    }

    public void onStop(@NotNull final Agent agent) {
      if (stopped) {
        return;
      }
      logger.dbg("[%s] stopping actor", actor);
      setStopped();
      try {
        behavior.onStop(agent);

      } catch (final Throwable t) {
        logger.wrn(t, "[%s] ignoring exception", actor);
        if (t instanceof InterruptedException) {
          Thread.currentThread().interrupt();

        } else if (t instanceof Error) {
          // rethrow errors
          throw (Error) t;
        }

      } finally {
        cancelTasks();
      }
    }

    void onRestart(@NotNull final Agent agent) {
      if (isDismissed()) {
        return;
      }
      final Behavior behavior = StandardAgent.this.behavior;
      try {
        behavior.onStop(agent);
        behavior.onStart(agent);

      } catch (final Throwable t) {
        setStopped();
        cancelTasks();
        logger.wrn(t, "[%s] ignoring exception", actor);
        if (t instanceof InterruptedException) {
          Thread.currentThread().interrupt();

        } else if (t instanceof Error) {
          // rethrow errors
          throw (Error) t;
        }
      }
    }
  }
}
