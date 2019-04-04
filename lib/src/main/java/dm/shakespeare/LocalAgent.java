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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;

import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Behavior.Agent;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.actor.Options;
import dm.shakespeare.concurrent.ActorExecutorService;
import dm.shakespeare.concurrent.ExecutorServices;
import dm.shakespeare.function.Observer;
import dm.shakespeare.log.Logger;
import dm.shakespeare.message.Bounce;
import dm.shakespeare.message.DeadLetter;
import dm.shakespeare.message.Delivery;
import dm.shakespeare.message.Failure;
import dm.shakespeare.util.ConstantConditions;

/**
 * Class implementing a local behavior agent.
 */
class LocalAgent implements Agent {

  private static final DeadLetter DEAD_LETTER = new DeadLetter();

  private final ActorExecutorService mActorExecutorService;
  private final Logger mLogger;
  private final HashSet<Actor> mObservers = new HashSet<Actor>();
  private final Observer<Actor> mRemover;

  private Actor mActor;
  private AgentExecutorService mAgentExecutorService;
  private AgentScheduledExecutorService mAgentScheduledExecutorService;
  private Behavior mBehavior;
  private BehaviorWrapper mBehaviorWrapper = new BehaviorStarter();
  private Runnable mDismissRunnable;
  private volatile boolean mDismissed;
  private Runnable mRestartRunnable;
  private volatile Thread mRunner;
  private boolean mStopped;

  /**
   * Creates a new agent instance.
   *
   * @param remover         the observer to be called when the actor is dismissed.
   * @param behavior        the initial behavior instance
   * @param executorService the backing executor service.
   * @param logger          the logger instance.
   */
  LocalAgent(@NotNull final Observer<Actor> remover, @NotNull final Behavior behavior,
      @NotNull final ExecutorService executorService, @NotNull final Logger logger) {
    mRemover = ConstantConditions.notNull("remover", remover);
    mBehavior = ConstantConditions.notNull("behavior", behavior);
    mActorExecutorService =
        (executorService instanceof ScheduledExecutorService) ? ExecutorServices.asActorExecutor(
            (ScheduledExecutorService) executorService)
            : ExecutorServices.asActorExecutor(executorService);
    mLogger = ConstantConditions.notNull("logger", logger);
  }

  public void dismissSelf() {
    mLogger.dbg("[%s] dismissing self", mActor);
    mDismissed = true;
    if (mDismissRunnable == null) {
      mDismissRunnable = new Runnable() {

        public void run() {
          mBehaviorWrapper.onStop(LocalAgent.this);
        }
      };
    }
    mActorExecutorService.executeNext(mDismissRunnable);
  }

  @NotNull
  public ExecutorService getExecutorService() {
    if (mAgentExecutorService == null) {
      final ActorExecutorService actorExecutorService = mActorExecutorService;
      if (actorExecutorService instanceof ScheduledExecutorService) {
        mAgentExecutorService = (mAgentScheduledExecutorService =
            new AgentScheduledExecutorService((ScheduledExecutorService) actorExecutorService,
                this));

      } else {
        mAgentExecutorService = new AgentExecutorService(actorExecutorService, this);
      }
    }
    return mAgentExecutorService;
  }

  @NotNull
  public Logger getLogger() {
    return mLogger;
  }

  @NotNull
  public ScheduledExecutorService getScheduledExecutorService() {
    if (mAgentScheduledExecutorService == null) {
      final ActorExecutorService actorExecutorService = mActorExecutorService;
      if (actorExecutorService instanceof ScheduledExecutorService) {
        mAgentExecutorService = (mAgentScheduledExecutorService =
            new AgentScheduledExecutorService((ScheduledExecutorService) actorExecutorService,
                this));

      } else {
        mAgentScheduledExecutorService =
            new AgentScheduledExecutorService(ExecutorServices.asScheduled(mActorExecutorService),
                this);
      }
    }
    return mAgentScheduledExecutorService;
  }

  @NotNull
  public Actor getSelf() {
    return mActor;
  }

  public boolean isDismissed() {
    return mStopped || mDismissed;
  }

  public void restartSelf() {
    mLogger.dbg("[%s] restarting self", mActor);
    if (mRestartRunnable == null) {
      mRestartRunnable = new Runnable() {

        public void run() {
          final Thread runner = (mRunner = Thread.currentThread());
          try {
            mBehaviorWrapper.onRestart(LocalAgent.this);

          } finally {
            mRunner = null;
          }

          if (runner.isInterrupted()) {
            mLogger.wrn("[%s] thread has been interrupted!", mActor);
            mBehaviorWrapper.onStop(LocalAgent.this);
          }
        }
      };
    }
    mActorExecutorService.executeNext(mRestartRunnable);
  }

  public void setBehavior(@NotNull final Behavior behavior) {
    mLogger.dbg("[%s] setting new behavior: behavior=%s", mActor, behavior);
    mBehavior = ConstantConditions.notNull("behavior", behavior);
  }

  void addObserver(@NotNull final Actor observer) {
    if (mStopped) {
      reply(observer, DEAD_LETTER, null);

    } else {
      mObservers.add(observer);
    }
  }

  void dismiss(final boolean mayInterruptIfRunning) {
    if (mayInterruptIfRunning) {
      final Thread runner = mRunner;
      if (runner != null) {
        runner.interrupt();
      }
    }
    dismissSelf();
  }

  @NotNull
  ActorExecutorService getActorExecutorService() {
    return mActorExecutorService;
  }

  void message(final Object message, @NotNull final Envelop envelop) {
    final Thread runner = (mRunner = Thread.currentThread());
    try {
      mBehaviorWrapper.onMessage(message, envelop, this);

    } finally {
      mRunner = null;
    }

    if (runner.isInterrupted()) {
      mLogger.wrn("[%s] thread has been interrupted!", mActor);
      mBehaviorWrapper.onStop(this);
    }
  }

  void messages(@NotNull final Iterable<?> messages, @NotNull final Envelop envelop) {
    final Options options = envelop.getOptions();
    if (isDismissed() && (options.getReceiptId() != null)) {
      final ArrayList<Object> bounces = new ArrayList<Object>();
      for (final Object message : messages) {
        bounces.add(new Bounce(message, options));
      }
      replyAll(envelop.getSender(), bounces, options.threadOnly());
      return;
    }
    final Thread runner = (mRunner = Thread.currentThread());
    try {
      for (final Object message : messages) {
        mBehaviorWrapper.onMessage(message, envelop, this);
      }

    } finally {
      mRunner = null;
    }

    if (runner.isInterrupted()) {
      mLogger.wrn("[%s] thread has been interrupted!", mActor);
      mBehaviorWrapper.onStop(this);
    }
  }

  void removeObserver(@NotNull final Actor observer) {
    mObservers.remove(observer);
  }

  void reply(@NotNull final Actor actor, final Object message, @Nullable final Options options) {
    final Actor self = mActor;
    mLogger.dbg("[%s] replying: actor=%s - options=%s - message=%s", self, actor, options, message);
    try {
      actor.tell(message, options, self);

    } catch (final RejectedExecutionException e) {
      mLogger.err(e, "[%s] ignoring exception", self);
    }
  }

  void replyAll(@NotNull final Actor actor, @NotNull final Iterable<?> messages,
      @Nullable final Options options) {
    final Actor self = mActor;
    mLogger.dbg("[%s] replying all: actor=%s - options=%s - message=%s", self, actor, options,
        messages);
    try {
      actor.tellAll(messages, options, self);

    } catch (final RejectedExecutionException e) {
      mLogger.err(e, "[%s] ignoring exception", self);
    }
  }

  void setActor(@NotNull final Actor actor) {
    mActor = ConstantConditions.notNull("actor", actor);
  }

  private void cancelTasks() {
    final AgentExecutorService executorService = mAgentExecutorService;
    if (executorService != null) {
      executorService.cancelAll(true);
    }
    final AgentScheduledExecutorService scheduledExecutorService = mAgentScheduledExecutorService;
    if (scheduledExecutorService != null) {
      scheduledExecutorService.cancelAll(true);
    }
  }

  private void setStopped() {
    mStopped = true;
    try {
      mRemover.accept(mActor);

    } catch (final Exception e) {
      // it should never happen
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }

      throw new RuntimeException(e);
    }

    for (final Actor observer : mObservers) {
      reply(observer, DEAD_LETTER, null);
    }
  }

  private class BehaviorStarter extends BehaviorWrapper {

    @Override
    public void onMessage(final Object message, @NotNull final Envelop envelop,
        @NotNull final Agent agent) {
      mBehaviorWrapper = new BehaviorWrapper();
      super.onStart(agent);
      super.onMessage(message, envelop, agent);
    }

    @Override
    public void onStop(@NotNull final Agent agent) {
    }
  }

  private class BehaviorWrapper implements Behavior {

    public void onMessage(final Object message, @NotNull final Envelop envelop,
        @NotNull final Agent agent) {
      mLogger.dbg("[%s] handling new message: envelop=%s - message=%s", mActor, envelop, message);
      if (isDismissed()) {
        final Options options = envelop.getOptions();
        if (options.getReceiptId() != null) {
          reply(envelop.getSender(), new Bounce(message, options), options.threadOnly());
        }
        return;
      }
      final Options options = envelop.getOptions();
      if (options.getReceiptId() != null) {
        try {
          mBehavior.onMessage(message, envelop, agent);
          if (!envelop.isPreventReceipt()) {
            reply(envelop.getSender(), new Delivery(message, options), options.threadOnly());
          }

        } catch (final Throwable t) {
          if (!envelop.isPreventReceipt()) {
            reply(envelop.getSender(), new Failure(message, options, t), options.threadOnly());
          }
          onStop(agent);
          if (t instanceof InterruptedException) {
            Thread.currentThread().interrupt();
          }
        }

      } else {
        try {
          mBehavior.onMessage(message, envelop, agent);

        } catch (final Throwable t) {
          onStop(agent);
          if (t instanceof InterruptedException) {
            Thread.currentThread().interrupt();
          }
        }
      }
    }

    public void onStart(@NotNull final Agent agent) {
      mLogger.dbg("[%s] staring actor", mActor);
      try {
        mBehavior.onStart(agent);

      } catch (final Throwable t) {
        setStopped();
        cancelTasks();
        mLogger.wrn(t, "[%s] ignoring exception", mActor);
        if (t instanceof InterruptedException) {
          Thread.currentThread().interrupt();
        }
      }
    }

    public void onStop(@NotNull final Agent agent) {
      if (mStopped) {
        return;
      }
      mLogger.dbg("[%s] stopping actor", mActor);
      setStopped();
      try {
        mBehavior.onStop(agent);

      } catch (final Throwable t) {
        mLogger.wrn(t, "[%s] ignoring exception", mActor);
        if (t instanceof InterruptedException) {
          Thread.currentThread().interrupt();
        }

      } finally {
        cancelTasks();
      }
    }

    void onRestart(@NotNull final Agent agent) {
      if (isDismissed()) {
        return;
      }
      final Behavior behavior = mBehavior;
      try {
        behavior.onStop(agent);
        behavior.onStart(agent);

      } catch (final Throwable t) {
        setStopped();
        cancelTasks();
        mLogger.wrn(t, "[%s] ignoring exception", mActor);
        if (t instanceof InterruptedException) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }
}
