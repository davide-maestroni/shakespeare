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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import dm.shakespeare.actor.Actor;
import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Behavior.Agent;
import dm.shakespeare.log.Logger;
import dm.shakespeare.util.ConstantConditions;

/**
 * {@link Agent} decorator class.
 */
public class AgentWrapper implements Agent {

  private final Agent agent;

  /**
   * Creates a new wrapper decorating the specified agent.
   *
   * @param agent the agent instance.
   */
  public AgentWrapper(@NotNull final Agent agent) {
    this.agent = ConstantConditions.notNull("agent", agent);
  }

  /**
   * {@inheritDoc}
   */
  @NotNull
  public ExecutorService getExecutorService() {
    return agent.getExecutorService();
  }

  /**
   * {@inheritDoc}
   */
  @NotNull
  public Logger getLogger() {
    return agent.getLogger();
  }

  /**
   * {@inheritDoc}
   */
  @NotNull
  public ScheduledExecutorService getScheduledExecutorService() {
    return agent.getScheduledExecutorService();
  }

  /**
   * {@inheritDoc}
   */
  @NotNull
  public Actor getSelf() {
    return agent.getSelf();
  }

  /**
   * {@inheritDoc}
   */
  public boolean isDismissed() {
    return agent.isDismissed();
  }

  /**
   * {@inheritDoc}
   */
  public void restartBehavior() {
    agent.restartBehavior();
  }

  /**
   * {@inheritDoc}
   */
  public void setBehavior(@NotNull final Behavior behavior) {
    agent.setBehavior(behavior);
  }
}
