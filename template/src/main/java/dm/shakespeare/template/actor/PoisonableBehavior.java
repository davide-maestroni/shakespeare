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

import java.io.IOException;
import java.io.ObjectInputStream;

import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.actor.SerializableBehavior;
import dm.shakespeare.template.config.BuildConfig;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 03/24/2019.
 */
public class PoisonableBehavior implements SerializableBehavior {

  public static final Object POISON_PILL = new Object();

  private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

  private transient PoisonableAgentWrapper agent = new PoisonableAgentWrapper();

  private Behavior behavior;

  PoisonableBehavior(@NotNull final Behavior behavior) {
    this.behavior = ConstantConditions.notNull("behavior", behavior);
  }

  public void onMessage(final Object message, @NotNull final Envelop envelop,
      @NotNull final Agent agent) throws Exception {
    if (message == POISON_PILL) {
      agent.dismissSelf();
      return;
    }
    behavior.onMessage(message, envelop, this.agent.withAgent(agent));
  }

  public void onStart(@NotNull final Agent agent) throws Exception {
    behavior.onStart(this.agent.withAgent(agent));
  }

  public void onStop(@NotNull final Agent agent) throws Exception {
    behavior.onStop(this.agent.withAgent(agent));
  }

  private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    agent = new PoisonableAgentWrapper();
  }

  private class PoisonableAgentWrapper extends AgentWrapper {

    @Override
    public void setBehavior(@NotNull final Behavior behavior) {
      PoisonableBehavior.this.behavior = ConstantConditions.notNull("behavior", behavior);
    }
  }
}
