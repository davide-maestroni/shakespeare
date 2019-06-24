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

package dm.shakespeare.actor;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import dm.shakespeare.actor.Behavior.Agent;
import dm.shakespeare.config.BuildConfig;
import dm.shakespeare.function.Mapper;
import dm.shakespeare.log.Logger;
import dm.shakespeare.util.ConstantConditions;

/**
 * Serializable implementation of a {@code Role}.<br>
 * The role instance will be serializable only if all the behavior instances, set during the actor
 * lifecycle, effectively are.
 */
public abstract class SerializableRole extends Role implements Serializable {

  private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

  private static Behavior NO_OP = new Behavior() {

    public void onMessage(final Object message, @NotNull final Envelop envelop,
        @NotNull final Agent agent) {
    }

    public void onStart(@NotNull final Agent agent) {
    }

    public void onStop(@NotNull final Agent agent) {
    }
  };

  private Behavior behavior;
  private RoleState state = RoleState.CREATED;

  /**
   * Creates a new role from the specified mapper.<br>
   * The mapper will be called passing the actor ID as input parameter and must return a behavior
   * instance.
   *
   * @param mapper the mapper function.
   * @return the role instance.
   */
  @NotNull
  public static SerializableRole from(
      @NotNull final Mapper<? super String, ? extends Behavior> mapper) {
    ConstantConditions.notNull("mapper", mapper);
    return new SerializableRole() {

      @NotNull
      protected Behavior getSerializableBehavior(@NotNull final String id) throws Exception {
        return mapper.apply(id);
      }
    };
  }

  /**
   * {@inheritDoc}
   */
  @NotNull
  @Override
  public final Behavior getBehavior(@NotNull final String id) throws Exception {
    final Behavior behavior = this.behavior;
    return new BehaviorWrapper((behavior != null) ? behavior : getSerializableBehavior(id));
  }

  /**
   * Returns the stored behavior.<br>
   * Usually needed during serialization.
   *
   * @return the behavior instance.
   */
  public Behavior getBehavior() {
    return behavior;
  }

  /**
   * Returns the stored state.<br>
   * Usually needed during serialization.
   *
   * @return the state type.
   */
  public RoleState getState() {
    return state;
  }

  /**
   * Returns the initial actor behavior.<br>
   * The returned instance should be serializable according to the specific serializer which is
   * going to be employed.
   *
   * @param id the actor ID.
   * @return the behavior instance.
   * @throws Exception when an unexpected error occurs.
   */
  @NotNull
  protected abstract Behavior getSerializableBehavior(@NotNull final String id) throws Exception;

  /**
   * Enumeration defining the serializable role states.
   */
  public enum RoleState {

    /**
     * Created state.
     */
    CREATED(new BehaviorHandler() {

      @NotNull
      public Behavior onMessage(@NotNull final Behavior behavior) {
        throw new IllegalStateException("invalid role state: " + RoleState.CREATED);
      }

      @NotNull
      public Behavior onStart(@NotNull final Behavior behavior) {
        return behavior;
      }

      @NotNull
      public Behavior onStop(@NotNull final Behavior behavior) {
        return NO_OP;
      }
    }),

    /**
     * Started state.
     */
    STARTED(new BehaviorHandler() {

      @NotNull
      public Behavior onMessage(@NotNull final Behavior behavior) {
        return behavior;
      }

      @NotNull
      public Behavior onStart(@NotNull final Behavior behavior) {
        return NO_OP;
      }

      @NotNull
      public Behavior onStop(@NotNull final Behavior behavior) {
        return behavior;
      }
    }),

    /**
     * Stopped state.
     */
    STOPPED(new BehaviorHandler() {

      @NotNull
      public Behavior onMessage(@NotNull final Behavior behavior) {
        throw new IllegalStateException("invalid role state: " + RoleState.STOPPED);
      }

      @NotNull
      public Behavior onStart(@NotNull final Behavior behavior) {
        return behavior;
      }

      @NotNull
      public Behavior onStop(@NotNull final Behavior behavior) {
        return NO_OP;
      }
    }),

    /**
     * Dismissed state.
     */
    DISMISSED(new BehaviorHandler() {

      @NotNull
      public Behavior onMessage(@NotNull final Behavior behavior) {
        throw new IllegalStateException("invalid role state: " + RoleState.DISMISSED);
      }

      @NotNull
      public Behavior onStart(@NotNull final Behavior behavior) {
        throw new IllegalStateException("invalid role state: " + RoleState.DISMISSED);
      }

      @NotNull
      public Behavior onStop(@NotNull final Behavior behavior) {
        throw new IllegalStateException("invalid role state: " + RoleState.DISMISSED);
      }
    });

    private transient final BehaviorHandler handler;

    RoleState(@NotNull final BehaviorHandler handler) {
      this.handler = handler;
    }

    @NotNull
    private BehaviorHandler getBehaviorHandler() {
      return handler;
    }
  }

  private interface BehaviorHandler {

    @NotNull
    Behavior onMessage(@NotNull Behavior behavior);

    @NotNull
    Behavior onStart(@NotNull Behavior behavior);

    @NotNull
    Behavior onStop(@NotNull Behavior behavior);
  }

  private class AgentWrapper implements Agent {

    private final Agent agent;

    private AgentWrapper(@NotNull final Agent agent) {
      this.agent = agent;
    }

    @NotNull
    public ExecutorService getExecutorService() {
      return agent.getExecutorService();
    }

    @NotNull
    public Logger getLogger() {
      return agent.getLogger();
    }

    @NotNull
    public ScheduledExecutorService getScheduledExecutorService() {
      return agent.getScheduledExecutorService();
    }

    @NotNull
    public Actor getSelf() {
      return agent.getSelf();
    }

    public boolean isDismissed() {
      return agent.isDismissed();
    }

    public void restartSelf() {
      agent.restartSelf();
    }

    public void setBehavior(@NotNull final Behavior behavior) {
      SerializableRole.this.behavior = ConstantConditions.notNull("behavior", behavior);
      agent.setBehavior(behavior);
    }
  }

  private class BehaviorWrapper implements Behavior {

    private AgentWrapper agent;

    private BehaviorWrapper(@NotNull final Behavior behavior) {
      SerializableRole.this.behavior = ConstantConditions.notNull("behavior", behavior);
    }

    public void onMessage(final Object message, @NotNull final Envelop envelop,
        @NotNull final Agent agent) throws Exception {
      state.getBehaviorHandler().onMessage(behavior).onMessage(message, envelop, this.agent);
    }

    public void onStart(@NotNull final Agent agent) throws Exception {
      this.agent = new AgentWrapper(agent);
      final Behavior behavior = state.getBehaviorHandler().onStart(SerializableRole.this.behavior);
      state = RoleState.STARTED;
      behavior.onStart(this.agent);
    }

    public void onStop(@NotNull final Agent agent) throws Exception {
      final Behavior behavior = state.getBehaviorHandler().onStop(SerializableRole.this.behavior);
      state = RoleState.STOPPED;
      behavior.onStart(this.agent);
    }
  }
}
