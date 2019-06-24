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
import java.util.concurrent.ExecutorService;

import dm.shakespeare.actor.Behavior;
import dm.shakespeare.actor.Envelop;
import dm.shakespeare.actor.Role;
import dm.shakespeare.log.Logger;
import dm.shakespeare.template.behavior.AgentWrapper;
import dm.shakespeare.template.config.BuildConfig;
import dm.shakespeare.template.util.Reflections;
import dm.shakespeare.util.ConstantConditions;

/**
 * Created by davide-maestroni on 03/21/2019.
 */
public class RespawningRole extends Role implements Serializable {

  private static final Object[] NO_ARGS = new Object[0];

  private static final long serialVersionUID = BuildConfig.SERIAL_VERSION_UID;

  private final Object[] roleArgs;
  private final Class<? extends Role> roleClass;

  private transient Role role;

  public RespawningRole() {
    roleClass = RespawningRole.class;
    roleArgs = NO_ARGS;
  }

  public RespawningRole(@NotNull final Class<? extends Role> roleClass) {
    this.roleClass = ConstantConditions.notNull("roleClass", roleClass);
    roleArgs = NO_ARGS;
  }

  public RespawningRole(@NotNull final Class<? extends Role> roleClass,
      @NotNull final Object... roleArgs) {
    this.roleClass = ConstantConditions.notNull("roleClass", roleClass);
    this.roleArgs = ConstantConditions.notNull("roleArgs", roleArgs).clone();
  }

  @NotNull
  public Behavior getBehavior(@NotNull final String id) throws Exception {
    return new RestartingBehavior(getRoleInstance().getBehavior(id));
  }

  @NotNull
  @Override
  public ExecutorService getExecutorService(@NotNull final String id) throws Exception {
    return getRoleInstance().getExecutorService(id);
  }

  @NotNull
  @Override
  public Logger getLogger(@NotNull final String id) throws Exception {
    return getRoleInstance().getLogger(id);
  }

  @Override
  public int getQuota(@NotNull final String id) throws Exception {
    return getRoleInstance().getQuota(id);
  }

  // json
  public Object[] getRoleArgs() {
    return roleArgs.clone();
  }

  // json
  @NotNull
  public Class<? extends Role> getRoleClass() {
    return roleClass;
  }

  @NotNull
  private Role getRoleInstance() {
    if (role == null) {
      role = newRoleInstance();
    }
    return role;
  }

  @NotNull
  private Role newRoleInstance() {
    return Reflections.newInstance(roleClass, roleArgs);
  }

  private class RestartingBehavior implements Behavior {

    private RestartingAgentWrapper agent;

    private Behavior behavior;

    private RestartingBehavior(@NotNull final Behavior behavior) {
      this.behavior = ConstantConditions.notNull("behavior", behavior);
    }

    public void onMessage(final Object message, @NotNull final Envelop envelop,
        @NotNull final Agent agent) throws Exception {
      behavior.onMessage(message, envelop, this.agent);
    }

    public void onStart(@NotNull final Agent agent) throws Exception {
      this.agent = new RestartingAgentWrapper(agent);
      behavior.onStart(this.agent);
    }

    public void onStop(@NotNull final Agent agent) throws Exception {
      behavior.onStop(this.agent);
      if (!agent.isDismissed()) {
        behavior = newRoleInstance().getBehavior(agent.getSelf().getId());
      }
    }

    private class RestartingAgentWrapper extends AgentWrapper {

      private RestartingAgentWrapper(@NotNull final Agent agent) {
        super(agent);
      }

      @Override
      public void setBehavior(@NotNull final Behavior behavior) {
        RestartingBehavior.this.behavior = ConstantConditions.notNull("behavior", behavior);
      }
    }
  }
}
