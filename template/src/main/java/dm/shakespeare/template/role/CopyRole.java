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

package dm.shakespeare.template.role;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.actor.Role;
import dm.shakespeare.template.config.BuildConfig;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 03/21/2019.
 */
public class CopyRole extends ReferenceRole {

  private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

  public CopyRole(@NotNull final Class<? extends Role> roleClass) {
    super(roleClass);
  }

  public CopyRole(@NotNull final Class<? extends Role> roleClass,
      @NotNull final Serializable... roleArgs) {
    super(roleClass, roleArgs);
  }

  @NotNull
  protected Behavior getSerializableBehavior(@NotNull final String id) throws Exception {
    return new CopyBehavior(super.getBehavior(id));
  }

  private class CopyBehavior implements Behavior {

    private Behavior behavior;
    private boolean isStopped;

    private CopyBehavior(@NotNull final Behavior behavior) {
      this.behavior = ConstantConditions.notNull("behavior", behavior);
    }

    public void onMessage(final Object message, @NotNull final Envelop envelop,
        @NotNull final Agent agent) throws Exception {
      behavior.onMessage(message, envelop, agent);
    }

    public void onStart(@NotNull final Agent agent) throws Exception {
      if (isStopped) {
        isStopped = false;
        behavior = newRoleInstance().getBehavior(agent.getSelf().getId());
      }
      behavior.onStart(agent);
    }

    public void onStop(@NotNull final Agent agent) throws Exception {
      isStopped = true;
      behavior.onStop(agent);
    }
  }
}
