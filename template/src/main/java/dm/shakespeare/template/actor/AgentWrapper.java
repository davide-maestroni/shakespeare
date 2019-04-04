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

package dm.shakespeare.template.actor;

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

  private Agent mAgent;

  public void dismissSelf() {
    mAgent.dismissSelf();
  }

  @NotNull
  public ExecutorService getExecutorService() {
    return mAgent.getExecutorService();
  }

  @NotNull
  public Logger getLogger() {
    return mAgent.getLogger();
  }

  @NotNull
  public ScheduledExecutorService getScheduledExecutorService() {
    return mAgent.getScheduledExecutorService();
  }

  @NotNull
  public Actor getSelf() {
    return mAgent.getSelf();
  }

  public boolean isDismissed() {
    return mAgent.isDismissed();
  }

  public void restartSelf() {
    mAgent.restartSelf();
  }

  public void setBehavior(@NotNull final Behavior behavior) {
    mAgent.setBehavior(behavior);
  }

  @NotNull
  public AgentWrapper withAgent(final Agent agent) {
    mAgent = agent;
    return this;
  }
}
