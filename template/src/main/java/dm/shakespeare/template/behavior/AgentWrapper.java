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

/**
 * Created by davide-maestroni on 01/14/2019.
 */
public class AgentWrapper implements Agent {

  private Agent agent;

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
    agent.setBehavior(behavior);
  }

  @NotNull
  public AgentWrapper withAgent(final Agent agent) {
    this.agent = agent;
    return this;
  }
}
